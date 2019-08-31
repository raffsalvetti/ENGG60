#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "include/server.h"
#include "include/display.h"

void print_socket_event(int socket, char *message) {
  printf("socket %d => %s",socket, message);
  if(!strcmp(message, "echo\n")) {
    server_send(socket, "echo server!");
  }
  
}

void change_coodinates(int socket, char *message) {
  char *pc = strchr(message, ';');
  int x = 0, y = 0;
  if(!strcmp(message, "quit\n")) {
    test_display_quit();
  } else if(pc != NULL) {
    message[(int)(pc-message)] = '\0';
    x = atoi(message);
    y = atoi(pc + 1);
    test_display_set_xy(x, y);
  } else {
    fprintf(stderr, "Nao encontrei um ; na string: %s.\n", message);
  }
}

void change_coodinates_from_test_display(int x, int y) {
  fprintf(stdout, "ponto x=%d;y=%d gerado pelo display de teste\n", x, y);
}

int main(int argc, char *argv[]) {
  printf("Hell World!!\n");

  server_init(3200);
  server_add_on_error_callback(print_socket_event);
  server_add_on_connect_callback(print_socket_event);
  server_add_on_receive_callback(change_coodinates);
  server_add_on_send_callback(print_socket_event);
  server_add_on_disconnect_callback(print_socket_event);

  server_start();

  test_display_init();
  test_display_add_poin_generation_callback(change_coodinates_from_test_display);
  test_display_show();
  
  server_stop();

  return 0;
}
