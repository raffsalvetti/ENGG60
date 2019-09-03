#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "include/server.h"
#include "include/display.h"
#include "include/servo.h"

void print_socket_event(int socket, char *message) {
  printf("socket %d => %s",socket, message);
  if(!strcmp(message, "echo\n")) {
    server_send(socket, "echo server!");
  }
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
  } else {
    fprintf(stderr, "Nao encontrei coordenadas com o formato X;Y em: %s.\n", message);
  }
}

void change_coodinates_from_test_display(int x, int y) {
  fprintf(stdout, "ponto x=%d;y=%d gerado manualmente\n", x, y);
  servo_driver_set_x_position(x);
  servo_driver_set_y_position(y);
}

int main(int argc, char *argv[]) {
  printf("Hell World!!\n");
  char ip_address[15] = {'0','.','0','.','0','.','0'};

  server_init(3200);
  server_add_on_error_callback(print_socket_event);
  server_add_on_connect_callback(print_socket_event);
  server_add_on_receive_callback(change_coodinates);
  server_add_on_send_callback(print_socket_event);
  server_add_on_disconnect_callback(print_socket_event);
  server_get_ip_address(ip_address);
  fprintf(stdout, "endereco ip retornado: %s\n", ip_address);

  server_start();
  
  servo_driver_init();

  test_display_init();
  test_display_set_ip(ip_address);
  test_display_add_poin_generation_callback(change_coodinates_from_test_display);
  test_display_show();
  
  servo_driver_destroy();
  server_stop();

  return 0;
}
