#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>
#include <math.h>

#include "include/camdemo.h"

#define AMPLITUDE 50
#define RESOLUTION 100
#define CAMDEMO_START 0
#define CAMDEMO_STOP 100
#define STEP (CAMDEMO_STOP - CAMDEMO_START) / RESOLUTION

//#define USE_CIRCLE

static int running = 0;
static double t = 0;
static pthread_t thread_run;


camdemo_coord_generation_event coord_generation_handler;

void camdemo_set_on_coord_generation_handler(camdemo_coord_generation_event on_coord_generation) {
	coord_generation_handler = on_coord_generation;
}

static void inc() {
	t+=STEP;
}

static void dec() {
	t-=STEP;
}

static operation_handler op = inc;

typedef struct t_coord {
	int x;
	int y;
} t_coord;

static void gen_ponit(double t, t_coord *coord) {
	coord->x = t;
#ifdef USE_CIRCLE
	coord->y = 50 + (op == dec ? -1 : 1) * sqrt(pow(50, 2) - pow((t - 50), 2));
#else
	coord->y = AMPLITUDE + AMPLITUDE * (op == dec ? -1 : 1) * sin(M_PI * t * (M_PI / 105) + M_PI);
#endif
}

static void *execute() {
	t_coord coord;
	while(running) {
		if(t + STEP > CAMDEMO_STOP) {
			t = CAMDEMO_STOP;
			op = dec;
		}

		if(t - STEP < CAMDEMO_START) {
			t = CAMDEMO_START;
			op = inc;
		}
		op();
		gen_ponit(t, &coord);
		if(coord_generation_handler != NULL) {
			coord_generation_handler(coord.x, coord.y);
		}
		usleep(70*1000);
	}
	return NULL;
}

void camdemo_init() {

}

void camdemo_destroy() {

}

void camdemo_start() {
	running = 1;
	pthread_create(&thread_run, NULL, execute, NULL);
}

void camdemo_stop() {
	running = 0;
}
