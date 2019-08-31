IDIR =./include
CC=gcc
CFLAGS=-I$(IDIR) -Wall -g0 -O2

ALLEGRO_LIBS=$(shell pkg-config --cflags --libs allegro_acodec-5 allegro_audio-5 allegro_dialog-5 allegro_image-5 allegro_physfs-5 allegro_ttf-5 allegro_video-5 allegro-5 allegro_color-5 allegro_font-5 allegro_main-5 allegro_memfile-5 allegro_primitives-5)

ODIR=obj
LDIR =./lib

LIBS=-lm -lpthread $(ALLEGRO_LIBS)

_DEPS = server.h display.h pwm.h
DEPS = $(patsubst %,$(IDIR)/%,$(_DEPS))

_OBJ = main.o server.o display.o pwm.o
OBJ = $(patsubst %,$(ODIR)/%,$(_OBJ))


$(ODIR)/%.o: %.c $(DEPS)
	$(CC) -c -o $@ $< $(CFLAGS)

server: $(OBJ)
	$(CC) -o $@ $^ $(CFLAGS) $(LIBS)

.PHONY: clean

clean:
	rm -f $(ODIR)/*.o *~ core $(INCDIR)/*~
