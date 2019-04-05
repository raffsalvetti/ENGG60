import socket
import sys
import json
import time
import pygame
import threading

(width, height) = (640, 480)
localIP     = "10.75.1.56"
localPort   = 3200
bufferSize  = 1024

screen = None

running = True
position = (width/2, height/2)

def udp_server(host='127.0.0.1', port=1234):
    global position
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    print("Listening on udp %s:%s" % (host, port))
    s.bind((host, port))
    while running:
        (data, addr) = s.recvfrom(bufferSize)
        data = json.loads(data)
        position = ((width/2) + int(data[1] * width / 90), ((height/2) + int(data[2] * height / 90) ))
        # print("%r --> %r ====> %r" % (data, addr, position,))
        # position = position + data

def main():
    global running, position, screen

    # position = (width/2, height/2)

    t = threading.Thread(target=udp_server, args=[localIP, localPort])
    t.start()

    pygame.init()
    screen = pygame.display.set_mode((width, height))
    pygame.display.set_caption("Controller")

    while running:
        for event in pygame.event.get():
            # if event.type == pygame.MOUSEMOTION:
            #     position = pygame.mouse.get_pos()

            if event.type == pygame.QUIT:
                # portSocket.close()
                running = False

        screen.fill(pygame.Color("white"))
        drawAxis()
        pygame.draw.circle(screen, pygame.Color("blue"), position, 10)
        pygame.display.update()

def drawAxis():
    #horizontal axis
    pygame.draw.line(screen, pygame.Color("red"), (0, height/2), (width, height/2))
    #vertical axis
    pygame.draw.line(screen, pygame.Color("purple"), (width/2, 0), (width/2, height))

if __name__ == '__main__':
    main()
