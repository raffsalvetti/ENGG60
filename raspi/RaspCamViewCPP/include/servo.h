#ifndef __SERVO_DRIVER_H__
#define __SERVO_DRIVER_H__

#define SERVO_X_GPIO     4
#define SERVO_Y_GPIO     1

int servo_driver_init();
void servo_driver_set_x_position(int x);
void servo_driver_set_y_position(int y);
void servo_driver_destroy();

#endif
