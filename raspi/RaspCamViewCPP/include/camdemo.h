#ifndef __CAMDEMO_H__
#define __CAMDEMO_H__

typedef void (*operation_handler)(void);
typedef void(*camdemo_coord_generation_event)(int x, int y);

void camdemo_set_on_coord_generation_handler(camdemo_coord_generation_event on_coord_generation);
void camdemo_init();
void camdemo_destroy();
void camdemo_start();
void camdemo_stop();

#endif
