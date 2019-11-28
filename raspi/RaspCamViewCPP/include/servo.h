#ifndef __SERVO_DRIVER_H__
#define __SERVO_DRIVER_H__

#define SERVO_X_GPIO     4
#define SERVO_Y_GPIO     1

typedef struct t_center {
    int x;
    int y;
} t_center;

int servo_driver_init();
void servo_driver_set_x_position(int x);
void servo_driver_set_y_position(int y);
void servo_driver_calibrate();
void servo_driver_destroy();

#endif
