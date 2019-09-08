#include <unistd.h>
#include <stdio.h>
#include <sys/socket.h>
#include <stdlib.h>
#include <netinet/in.h>
#include <string.h>
#include <pthread.h>
#include <stdarg.h>
#include <errno.h>
#include <netdb.h>
#include <ifaddrs.h>

#include "include/server.h"

server_event error_handler, connect_handler, receive_handler, send_handler, disconnect_handler;

void server_add_on_error_callback(server_event error_callback) {
	error_handler = error_callback;
}

void server_add_on_connect_callback(server_event connect_callback) {
	connect_handler = connect_callback;
}

void server_add_on_receive_callback(server_event receive_callback) {
	receive_handler = receive_callback;
}

void server_add_on_send_callback(server_event send_callback) {
	send_handler = send_callback;
}

void server_add_on_disconnect_callback(server_event disconnect_callback) {
	disconnect_handler = disconnect_callback;
}

int tcp_server_fd, udp_server_fd;
struct sockaddr_in tcp_server_address, udp_server_address, udp_client_address;
char err_message_buffer[1024] = {0};
int active = 0;
pthread_t server_wait_for_connection_thread_id, server_udp_server_beacon_thread_id;

static void on_socket_error(int socket, char *message, ...) {
	if(error_handler != NULL) {
		va_list args;
		va_start(args, message);
		memset(err_message_buffer, 0, 1024);
		vsnprintf(err_message_buffer, 1024, message, args);
		va_end(args);
		error_handler(socket, err_message_buffer);
	}
}

static void on_socket_connect(int socket) {
	if(connect_handler != NULL) {
		memset(err_message_buffer, 0, 1024);
		sprintf(err_message_buffer, "Cliente conectado!\n");
		connect_handler(socket, err_message_buffer);
	}
}

static void on_socket_read(int socket, char *message) {
	if(receive_handler != NULL) {
		receive_handler(socket, message);
	}
}

static void on_socket_write(int socket, char *message) {
	if(send_handler != NULL) {
		send_handler(socket, err_message_buffer);
	}
}

static void on_socket_diconnect(int socket, char *message) {
	if(disconnect_handler != NULL) {
		disconnect_handler(socket, message);
	}
}

static void tcp_server_init(int port) {
	if ((tcp_server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
		on_socket_error(tcp_server_fd, "Nao foi possivel criar o socket!\n");
	}
	if (setsockopt(tcp_server_fd, SOL_SOCKET, SO_REUSEADDR | SO_REUSEPORT, &(int){1}, sizeof(int))) {
		on_socket_error(tcp_server_fd, "Nao foi possivel configurar o socket!\n");
	}
	tcp_server_address.sin_family = AF_INET;
	tcp_server_address.sin_addr.s_addr = INADDR_ANY;
	tcp_server_address.sin_port = htons(port);
	if (bind(tcp_server_fd, (struct sockaddr *)&tcp_server_address, sizeof(tcp_server_address)) < 0) {
		on_socket_error(tcp_server_fd, "Nao foi possivel ligar a porta %i ao socket!\n", port);
	}
	if (listen(tcp_server_fd, 3) < 0) {
		on_socket_error(tcp_server_fd, "Nao foi possivel escutar na porta %d!\n", port);
	}
}

static void *udp_server_beacon() {
	char udp_receive_buffer[REC_BUFFER_SIZE] = {0};
	int udp_socket_read_bytes;
	socklen_t len;
	char response[] = "DISCOVER_CONTROLLER_RESPONSE";
	while(active) {
		memset(udp_receive_buffer, 0, REC_BUFFER_SIZE);
		udp_socket_read_bytes = recvfrom(udp_server_fd, (char *)udp_receive_buffer, REC_BUFFER_SIZE, MSG_WAITALL, ( struct sockaddr *) &udp_client_address, &len);
		if(udp_socket_read_bytes != -1) {
			on_socket_read(udp_server_fd, udp_receive_buffer);
			if(strcmp("DISCOVER_CONTROLLER_REQUEST", udp_receive_buffer) == 0) {
				fprintf(stdout, "Tentando enviar %s\n", response);
				if(sendto(udp_server_fd, (const char *)response, strlen(response), MSG_CONFIRM, (const struct sockaddr *) &udp_client_address, len) != -1) {
					on_socket_write(udp_server_fd, response);
				} else {
					on_socket_error(udp_server_fd, "Erro enviando DISCOVER_CONTROLLER_RESPONSE: %s\n", strerror(errno));
				}
			}
		} else {
			on_socket_error(udp_server_fd, "Erro lendo DISCOVER CONTROLLER MESSAGE: %s\n", strerror(errno));
		}
	}
	return NULL;
}

static void udp_server_init(int port) {
	if ((udp_server_fd = socket(AF_INET, SOCK_DGRAM, 0)) < 0 ) {
		on_socket_error(udp_server_fd, "Nao foi possivel criar o servidor identificador de servico!\n");
	}
	memset(&udp_server_address, 0, sizeof(udp_server_address));
	memset(&udp_client_address, 0, sizeof(udp_client_address));
	udp_server_address.sin_family = AF_INET; // IPv4
	udp_server_address.sin_addr.s_addr = INADDR_ANY;
	udp_server_address.sin_port = htons(port);

	if (bind(udp_server_fd, (struct sockaddr *)&udp_server_address, sizeof(udp_server_address)) < 0) {
		on_socket_error(udp_server_fd, "Nao foi possivel ligar o servidor identificador de servico a porta %i!\n", port);
	}

}

void server_init(int port) {
	tcp_server_init(port);
	udp_server_init(port);
}

static void *server_wait_for_connection() { //TODO: aceita somente uma conexao, quem sabe modificar para aceitar varias conexoes no futuro?
	char tcp_receive_buffer[REC_BUFFER_SIZE] = {0};
	int tcp_socket_read_bytes = 0;
	int new_tcp_client;
	int addrlen = sizeof(tcp_server_address);
	while(active) {
		if ((new_tcp_client = accept(tcp_server_fd, (struct sockaddr *)&tcp_server_address, (socklen_t *)&addrlen)) < 0) {
			on_socket_error(tcp_server_fd, "Nao foi possivel aceitar conexao: %s\n", strerror(errno));
		} else {
			on_socket_connect(new_tcp_client);
			while(active) {
				memset(tcp_receive_buffer, 0, REC_BUFFER_SIZE);
				tcp_socket_read_bytes = read(new_tcp_client, tcp_receive_buffer, REC_BUFFER_SIZE);
				if(tcp_socket_read_bytes == -1) {
					on_socket_error(new_tcp_client, "Erro lendo dados: %s\n", strerror(errno));
					break;
				} else if(tcp_socket_read_bytes == 0) {
					if(strlen(tcp_receive_buffer) > 0)
						on_socket_read(new_tcp_client, tcp_receive_buffer);
					on_socket_diconnect(new_tcp_client, "Cliente desconectado!\n");
					break;
				} else {
					on_socket_read(new_tcp_client, tcp_receive_buffer);
				}
			}
		}
	}
	return NULL;
}

void server_send(int socket, char *data) {
	if(send(socket, data, strlen(data), 0) == -1) {
		on_socket_error(socket, "Erro enviando dados pelo socket!\n");
	} else {
		on_socket_write(socket, data);
	}
}

void server_start() {
	active = 1;
	pthread_create(&server_wait_for_connection_thread_id, NULL, server_wait_for_connection, NULL);
	pthread_create(&server_udp_server_beacon_thread_id, NULL, udp_server_beacon, NULL);
}

void server_stop() {
	active = 0;
}

void server_get_ip_address(char *ip_address) {
	struct ifaddrs *ifa, *ifaddr;
	int s;
	char host[NI_MAXHOST];
	if (getifaddrs(&ifaddr) == -1) {
		perror("getifaddrs");
		exit(EXIT_FAILURE);
	}
	for (ifa = ifaddr; ifa != NULL; ifa = ifa->ifa_next) {
		if (ifa->ifa_addr == NULL)
			continue;
		s = getnameinfo(ifa->ifa_addr,sizeof(struct sockaddr_in),host, NI_MAXHOST, NULL, 0, NI_NUMERICHOST);
		if((strcmp(ifa->ifa_name, "eth0")==0)&&(ifa->ifa_addr->sa_family==AF_INET)) {
			if (s != 0) {
				printf("getnameinfo() failed: %s\n", gai_strerror(s));
				exit(EXIT_FAILURE);
			}
			// printf("Network Interface Name %s\n",ifa->ifa_name);
			// printf("Network Address of %s\n",host);
			memcpy(ip_address, host, strlen(host));
		}
	}
	freeifaddrs(ifaddr);
}
