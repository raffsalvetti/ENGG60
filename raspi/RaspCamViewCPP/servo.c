#include <stdio.h>
#include <string.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <unistd.h>
#include "include/servo.h"

#define SERVOS_CALIBRATION_FILE "calibrated.bin"
#define SERVO_X_CENTER 50
#define SERVO_Y_CENTER 50

static int fd;
static int last_x = 0;
static int last_y = 0;
static t_center centro;
static char buffer[10];

static void load_center() {
    FILE *fp = fopen(SERVOS_CALIBRATION_FILE, "rb");
    if(fp == NULL) {
        fprintf(stderr, "nao foi possivel abrir arquivo de calibracao da camera "SERVOS_CALIBRATION_FILE"\n");
        centro.x = 0;
        centro.y = 0;
    } else {
#ifdef DEBUG
        fprintf(stdout, "lendo arquivo de calibracao\n");
#endif        
        fread(&centro, sizeof(t_center), 1, fp);
#ifdef DEBUG
        fprintf(stdout, "calibracao: x=%d; y=%d\n", centro.x, centro.y);
#endif        
        fclose(fp);
    }
}

static void save_center() {
    FILE *fp = fopen(SERVOS_CALIBRATION_FILE, "wb");
    if(fp == NULL) {
        fprintf(stderr, "nao foi possivel abrir arquivo de calibracao da camera "SERVOS_CALIBRATION_FILE"\n");
    } else {
#ifdef DEBUG
        fprintf(stdout, "escrevendo arquivo de calibracao\n");
#endif        
        fwrite(&centro, sizeof(t_center), 1, fp);
        fflush(fp);
        fclose(fp);
    }
}

static void write_to_dev(int servo_number, int position) {
    memset(buffer, 0, 10);
    sprintf(buffer, "%d=%d%%\n", servo_number, position);
    if(write(fd, buffer, strlen(buffer)) == -1) {
        buffer[strlen(buffer) - 1] = 0;
        fprintf(stderr, "nao consegui escrever a instrucao %s em /dev/servoblaster\n", buffer);
    } else {
#ifdef DEBUG
        fprintf(stdout, "enviando %s para /dev/servoblaster\n", buffer);
#endif
    }
}

void servo_driver_calibrate() {
    centro.x = last_x - SERVO_X_CENTER;
    centro.y = last_y - SERVO_Y_CENTER;
    save_center();
}

void servo_driver_set_x_position(int x) {
    x = (100 - x); //inversao da orientacao (montagem do motor invertida no casco)

    x = (x + centro.x); //aplicando calibracao

    if(x > 100) //filtro de limite maximo
        x = 100;
    
    if(x < 0) //filtro de limite minimo
        x = 0;

    if(last_x != x) {
        write_to_dev(SERVO_X_GPIO, x);
        last_x = x;
    }

}

void servo_driver_set_y_position(int y) {
    y = (y + centro.y);

    if(y > 100) //filtro de limite maximo
        y = 100;

    if(y < 0) //filtro de limite minimo
        y = 0;

    if(last_y != y && y >= 0 && y <= 100) {
        write_to_dev(SERVO_Y_GPIO, y);
        last_y = y;
    }
}

int servo_driver_init() {
    load_center();
    fd = open("/dev/servoblaster", O_WRONLY);
    if (fd < 0) {
        fprintf(stderr, "/dev/servoblaster não pode ser acessado. servoblaster foi instalado?\n");
        return 1;
    }
    return 0;
}

void servo_driver_destroy() {
    servo_driver_set_x_position(SERVO_X_CENTER);
    servo_driver_set_y_position(SERVO_Y_CENTER);
    close(fd);
}
