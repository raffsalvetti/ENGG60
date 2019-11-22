#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "include/server.h"
#include "include/display.h"
#include "include/servo.h"
#include "include/video.h"
#include "include/camdemo.h"

#define DEV
//#define TEST_DISPLAY
#define CAMDEMO

char ip_cliente[16] = {};

void print_socket_event(int socket, char *message) {
	printf("socket %d => %s",socket, message);
	char *pc = strchr(message, '#');
	if(pc != NULL) {
		strcpy(ip_cliente, (pc + 1));
		fprintf(stdout, "IP DO CLIETE:%s\n", ip_cliente);
	}
	// if(!strcmp(message, "echo\n")) {
	//   server_send(socket, "echo server!");
	// }
}

void sock_disconnect(int socket, char *message) {
	video_stop();
	fprintf(stdout, "Socket %d desconectado: %s\n", socket, message);
}

void sock_error(int socket, char *message) {
	video_stop();
	fprintf(stderr, "Erro no socket %d: %s\n", socket, message);
}

void change_coodinates(int socket, char *message) {
	char *pc = strchr(message, ';');
	int x = 0, y = 0;
	if(pc != NULL) {
		message[(int)(pc-message)] = '\0';
		x = atoi(message);
		y = atoi(pc + 1);
		test_display_set_xy(x, y);
		servo_driver_set_x_position(x);
		servo_driver_set_y_position(y);
	} else if(!strcmp(message, "VIDEOSTART") || !strcmp(message, "VIDEOSTART\n")) {
		video_start(ip_cliente);
	} else if(!strcmp(message, "VIDEOSTOP") || !strcmp(message, "VIDEOSTOP\n")) {
		video_stop();
	} else if(!strcmp(message, "DISCOVER_CONTROLLER_REQUEST")) {
		fprintf(stdout, "Negociando conexão com cliente!\n");
	} else {
		fprintf(stderr, "Nao encontrei coordenadas com o formato X;Y em: %s.\n", message);
	}
}

void change_coodinates_from_test_display(int x, int y) {
	fprintf(stdout, "ponto x=%d;y=%d gerado manualmente\n", x, y);
	servo_driver_set_x_position(x);
	servo_driver_set_y_position(y);
}

void change_coordinates_from_camdemo(int x, int y){
	fprintf(stdout, "ponto x=%d;y=%d gerado pelo camdemo\n", x, y);
	servo_driver_set_x_position(x);
	servo_driver_set_y_position(y);
	test_display_set_xy(x, y);
}

int main(int argc, char *argv[]) {
	printf("Hell World!!\n");
	char ip_address[15] = {'0','.','0','.','0','.','0'};

	server_init(3200);
	server_add_on_error_callback(sock_error);
	server_add_on_connect_callback(print_socket_event);
	server_add_on_receive_callback(change_coodinates);
	server_add_on_send_callback(print_socket_event);
	server_add_on_disconnect_callback(sock_disconnect);
	server_get_ip_address(ip_address);
	fprintf(stdout, "endereco ip retornado: %s\n", ip_address);

	camdemo_set_on_coord_generation_handler(change_coordinates_from_camdemo);

	server_start();
	servo_driver_init();
	camdemo_init();

#ifdef CAMDEMO
	camdemo_start();
#endif

#ifdef TEST_DISPLAY
	test_display_init();
	test_display_set_ip(ip_address);
	test_display_add_poin_generation_callback(change_coodinates_from_test_display);
	test_display_show();
#else
	char op = 'N';
	while(op != 'S') {
		fprintf(stdout, "Para sair digite S a qualqer momento.\n");
		op = getchar();
	}
#endif

	servo_driver_destroy();
	server_stop();

#ifdef CAMDEMO
	camdemo_stop();
#endif

	return 0;
}
