import socket
import sys
import json
import time
import pygame
import threading
import errno

(width, height) = (640, 480)
localIP     = ""
localPort   = 3200
bufferSize  = 128*1024

screen = None

running = True
position = (width/2, height/2)

def udp_server(host='127.0.0.1', port=1234):
    global position
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, True)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, True)
    s.setblocking(False)
    print("Servidor em %s:%s" % (host, port))
    s.bind((host, port))
    for ip in socket.gethostbyname_ex(socket.gethostname()):
        print "eu sou: %s" % ip
    while running:
        try:
            (data, addr) = s.recvfrom(bufferSize)
            if data:
                print data
                data = json.loads(data)
                #if data.t != None and data.t == 'w': #whois
                #    s.sendto('Im_the_master_of_the_universe', addr)
                #elif data.t != None and data.t == 'p': #position
                position = ((width/2) + int(data.p[1] * width / 90), ((height/2) + int(data.p[2] * height / 90) ))
        except socket.error, ex:
            if ex.errno != errno.EAGAIN:
                raise ex


def main():
    global running, position, screen

    t = threading.Thread(target=udp_server, args=[localIP, localPort])
    t.start()

    pygame.init()
    screen = pygame.display.set_mode((width, height))
    pygame.display.set_caption("Controller")

    while running:
        for event in pygame.event.get():
            # if event.type == pygame.MOUSEMOTION:
            #     position = pygame.mouse.get_pos()

            if event.type == pygame.QUIT or (event.type == pygame.KEYDOWN and event.key == pygame.K_ESCAPE):
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
