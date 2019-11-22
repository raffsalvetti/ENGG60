EXEC_NAME=controlador

IDIR =./include
#CC=gcc
CC=gcc
FLAGS=-I$(IDIR) -Wall -g3 -O0 $(CFLAGS)

ALLEGRO_LIBS=$(shell pkg-config --cflags --libs allegro_acodec-5 allegro_audio-5 allegro_dialog-5 allegro_image-5 allegro_physfs-5 allegro_ttf-5 allegro_video-5 allegro-5 allegro_color-5 allegro_font-5 allegro_main-5 allegro_memfile-5 allegro_primitives-5)

ODIR=obj
LDIR =./lib

LIBS=-lm -lpthread $(ALLEGRO_LIBS)

_DEPS = server.h display.h servo.h video.h camdemo.h
DEPS = $(patsubst %,$(IDIR)/%,$(_DEPS))

_OBJ = main.o server.o display.o servo.o video.o camdemo.o
OBJ = $(patsubst %,$(ODIR)/%,$(_OBJ))

all: $(EXEC_NAME)

$(ODIR)/%.o: %.c $(DEPS)
	$(CC)  -c -o $@ $< $(FLAGS)

$(EXEC_NAME): $(OBJ)
	$(CC) -o $@ $^ $(FLAGS) $(LIBS)

.PHONY: clean, all

clean:
	rm -f $(ODIR)/*.o *~ $(EXEC_NAME) $(INCDIR)/*~
