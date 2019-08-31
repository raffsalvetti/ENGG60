#include <stdio.h>
#include <math.h>
#include <allegro5/allegro.h>
#include <allegro5/allegro_primitives.h>
#include "include/display.h"

const float _FPS = 60;

// int _x = WIDTH / 2, _y = HEIGHT / 2;
ALLEGRO_DISPLAY *display = NULL;
ALLEGRO_EVENT_QUEUE *event_queue = NULL;
ALLEGRO_TIMER *timer = NULL;
ALLEGRO_MOUSE_STATE mouse_state;
ALLEGRO_COLOR color_white, color_green;
bool running = true;
bool redraw = true;
test_display_poin_generation_event poin_generation_handler;
my_cicrle target;


void test_display_set_xy(int x, int y)
{
    if(!target.selected) {
        target.x = (WIDTH / 2) + (x * (WIDTH / 2))/100;
        target.y = (HEIGHT / 2) + (y * (HEIGHT / 2))/100;
    }
}

void test_display_quit()
{
    running = false;
}

int test_display_init()
{
    target.x = (WIDTH / 2);
    target.y = (HEIGHT / 2);
    target.radius = 15;
    target.selected = 0;
    
    // Initialize allegro
    if (!al_init())
    {
        fprintf(stderr, "Failed to initialize allegro.\n");
        return 1;
    }
    if (!al_install_mouse())
    {
        fprintf(stderr, "Failed to initialize mouse.\n");
        return 1;
    }

    if (!al_init_primitives_addon())
    {
        fprintf(stderr, "Failed to initialize allegro primitives addons.\n");
        return 1;
    }

    // Initialize the timer
    timer = al_create_timer(1.0 / _FPS);
    if (!timer)
    {
        fprintf(stderr, "Failed to create timer.\n");
        return 1;
    }

    // Create the display
    display = al_create_display(WIDTH, HEIGHT);
    if (!display)
    {
        fprintf(stderr, "Failed to create display.\n");
        return 1;
    }

    // Create the event queue
    event_queue = al_create_event_queue();
    if (!event_queue)
    {
        fprintf(stderr, "Failed to create event queue.");
        return 1;
    }

    // Register event sources
    al_register_event_source(event_queue, al_get_display_event_source(display));
    al_register_event_source(event_queue, al_get_timer_event_source(timer));
    al_register_event_source(event_queue, al_get_mouse_event_source());

    color_white = al_map_rgb(255, 255, 255);
    color_green = al_map_rgb(69, 171, 69);

    // Display a black screen
    al_clear_to_color(al_map_rgb(0, 0, 0));
    al_flip_display();

    // Start the timer
    al_start_timer(timer);
}

static void test_display_destroy()
{
    al_destroy_display(display);
    al_destroy_event_queue(event_queue);
}

static int is_inside_circle(ALLEGRO_MOUSE_STATE mouse_state) {
    return (sqrt( pow((mouse_state.x - target.x), 2) + pow((mouse_state.y - target.y), 2) ) <= target.radius);
}

static void on_point_generation(int x, int y) {
    if(poin_generation_handler != NULL) {
        poin_generation_handler(x, y);
    }
}

void test_display_show()
{
    while (running)
    {
        ALLEGRO_EVENT event;
        ALLEGRO_TIMEOUT timeout;

        // Initialize timeout
        al_init_timeout(&timeout, 0.06);

        // Fetch the event (if one exists)
        bool get_event = al_wait_for_event_until(event_queue, &event, &timeout);

        // Handle the event
        if (get_event)
        {
            switch (event.type)
            {
            case ALLEGRO_EVENT_MOUSE_BUTTON_UP:
                target.selected = 0;
                break;

            case ALLEGRO_EVENT_MOUSE_BUTTON_DOWN:
            case ALLEGRO_EVENT_MOUSE_AXES:
                al_get_mouse_state(&mouse_state);
                if (mouse_state.buttons & 1)
                {
                    if(target.selected || is_inside_circle(mouse_state)) {
                        target.selected = 1;
                        target.x = mouse_state.x;
                        target.y = mouse_state.y;
                        on_point_generation((100 * (target.x - (WIDTH / 2))) / (WIDTH / 2), (100 * (target.y - (HEIGHT / 2))) / (HEIGHT / 2));
                    }
                    // fprintf(stdout, "Mouse position: (%d, %d)\n", (100 * (mouse_state.x - (WIDTH / 2))) / (WIDTH / 2), (100 * (mouse_state.y - (HEIGHT / 2))) / (HEIGHT / 2));
                }
                break;

            case ALLEGRO_EVENT_TIMER:
                redraw = true;
                break;
            case ALLEGRO_EVENT_DISPLAY_CLOSE:
                running = false;
                break;
            default:
                // fprintf(stderr, "Unsupported event received: %d\n", event.type);
                break;
            }
        }

        // Check if we need to redraw
        if (redraw && al_is_event_queue_empty(event_queue))
        {
            // Redraw
            al_clear_to_color(al_map_rgb(0, 0, 0));

            al_draw_line((float)(WIDTH / 2), (float)0, (float)(WIDTH / 2), HEIGHT, color_white, (float)1);
            al_draw_line((float)0, (float)(HEIGHT / 2), WIDTH, (float)(HEIGHT / 2), color_white, (float)1);

            al_draw_filled_circle((float)target.x, (float)target.y, (float)target.radius, target.selected ? color_green : color_white);
            // al_draw_textf(const ALLEGRO_FONT *font, ALLEGRO_COLOR color, float x, float y, int flags, const char *format, ...);

            al_flip_display();
            redraw = false;
        }
    }

    test_display_destroy();
}


void test_display_add_poin_generation_callback(test_display_poin_generation_event poin_generation_callback) {
    poin_generation_handler = poin_generation_callback;
}