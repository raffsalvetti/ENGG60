#include <unistd.h>
#include <stdio.h>
#include <sys/socket.h>
#include <stdlib.h>
#include <netinet/in.h>
#include <string.h>
#include <pthread.h>
#include <stdarg.h>

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

int server_fd, new_socket, valread;
struct sockaddr_in address;
int addrlen = sizeof(address);
char buffer[REC_BUFFER_SIZE] = {0};
char err_message_buffer[1024] = {0};
int active = 0;
pthread_t server_wait_for_connection_thread_id;

static void on_socket_error(int socket, char *message, ...) {
	if(error_handler != NULL) {
		va_list args;
		va_start(args, message);
		memset(err_message_buffer, 0, 1024);
		sprintf(err_message_buffer, message, args);
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

void server_init(int port) {
	if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
		on_socket_error(server_fd, "Nao foi possivel criar o socket!\n");
	}
	if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR | SO_REUSEPORT, &(int){1}, sizeof(int))) {
		on_socket_error(server_fd, "Nao foi possivel configurar o socket!\n");
	}
	address.sin_family = AF_INET;
	address.sin_addr.s_addr = INADDR_ANY;
	address.sin_port = htons(port);
	if (bind(server_fd, (struct sockaddr *)&address, sizeof(address)) < 0) {
		on_socket_error(server_fd, "Nao foi possivel ligar a porta %i ao socket!\n", port);
	}
	if (listen(server_fd, 3) < 0) {
		on_socket_error(server_fd, "Nao foi possivel escutar na porta %d!\n", port);
	}
}

static void *server_wait_for_connection() { //TODO: aceita somente uma conexao, quem sabe modificar para aceitar varias conexoes no futuro?
	while(active) {
		if ((new_socket = accept(server_fd, (struct sockaddr *)&address, (socklen_t *)&addrlen)) < 0) {
			on_socket_error(server_fd, "Nao foi possivel aceitar conexao!\n");
		} else {
			on_socket_connect(new_socket);
			while(active) {
				memset(buffer, 0, REC_BUFFER_SIZE);
				valread = read(new_socket, buffer, REC_BUFFER_SIZE);
				if(valread == -1) {
					on_socket_error(new_socket, "Erro lendo dados!\n");
					break;
				} else if(valread == 0) {
					if(strlen(buffer) > 0)
						on_socket_read(new_socket, buffer);
					on_socket_diconnect(new_socket, "Cliente desconectado!\n");
					break;
				} else {
					on_socket_read(new_socket, buffer);
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
	// pthread_join(server_wait_for_connection_thread_id, NULL);
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
