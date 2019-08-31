#ifndef __SERVER_H_
#define __SERVER_H_

#define REC_BUFFER_SIZE 1024*8
typedef void (*server_event)(int socket, char *message);
void server_add_on_error_callback(server_event error_callback);
void server_add_on_connect_callback(server_event connect_callback);
void server_add_on_receive_callback(server_event receive_callback);
void server_add_on_send_callback(server_event send_callback);
void server_add_on_disconnect_callback(server_event disconnect_callback);
void server_init(int port);
void server_send(int socket, char *data);
void server_start();
void server_stop();

#endif
