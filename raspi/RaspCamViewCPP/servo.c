#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <unistd.h>
#include "include/servo.h"

int fd;
int last_x = 0;
int last_y = 0;

static void write_to_dev(int servo_number, int position) {
    char buffer[10];
    memset(buffer, 0, 10);
    sprintf(buffer, "%d=%d%%%%", servo_number, position);
    if(write(fd, buffer, strlen(buffer)) == -1)
        fprintf(stderr, "nao consegui mandar o comando %s para /dev/servoblaster\n", buffer);
}

void servo_driver_set_x_position(int x) {
    if(last_x != x){
        write_to_dev(SERVO_X_GPIO, x);
        last_x = x;
    }
    
}

void servo_driver_set_y_position(int y) {
    if(last_y != y) {
        write_to_dev(SERVO_Y_GPIO, y);
        last_y = y;
    }
}

int servo_driver_init() {
    fd = open("/dev/servoblaster", O_WRONLY);
    if (fd < 0) {
        fprintf(stderr, "/dev/servoblaster não pode ser acessado. servoblaster foi instalado?\n");
        return 1;
    }
    return 0;
}

void servo_driver_destroy() {
    servo_driver_set_x_position(0);
    servo_driver_set_y_position(0);
    close(fd);
}