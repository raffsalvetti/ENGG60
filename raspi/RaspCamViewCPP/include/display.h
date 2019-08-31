#ifndef __DISPLAY_H__
#define __DISPLAY_H__

#define WIDTH 640
#define HEIGHT 480

typedef struct {
    int x;
    int y;
    int radius;
    char selected;
} my_cicrle;

typedef void(*test_display_poin_generation_event)(int x, int y);

void test_display_set_xy(int x, int y);
void test_display_quit();
int test_display_init();
void test_display_show();
void test_display_add_poin_generation_callback(test_display_poin_generation_event poin_generation_callback);

#endif