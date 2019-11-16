#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>

#include "include/video.h"

int video_pid = -666;

void video_start(char *client_ip_address) {
  if(video_pid != -666 && video_pid != -1 && video_pid != 0)
    kill(video_pid, SIGTERM);

  video_pid = fork();

  if(video_pid == -1) {
    fprintf(stdout, "Impossivel criar processo de video!\n");
  } else if(video_pid == 0) {
    int buffer_size = 11 + strlen(client_ip_address) + 1;
    char *buffer = (char*)malloc(buffer_size * sizeof(char));
    memset(buffer, 0, buffer_size);
    sprintf(buffer, "rtp://%s:8888", client_ip_address);
    // char *args[] = {"-f", "v4l2", "-i", "/dev/video0", "-vcodec", "h264_omx", "-r", "30", "-b:v", "256k", "-s", "320x240", "-f", "rtp", "-reorder_queue_size", "0", buffer, NULL};
    #ifdef __arm__
      execl("/usr/bin/ffmpeg", "/usr/bin/ffmpeg", "-f", "v4l2", "-i", "/dev/video0", "-vcodec", "h264_omx", "-r", "30", "-b:v", "256k", "-s", "320x240", "-f", "rtp", "-reorder_queue_size", "0", buffer, NULL);
    #else
      execl("/usr/bin/ffmpeg", "/usr/bin/ffmpeg", "-f", "v4l2", "-i", "/dev/video0", "-vcodec", "h264", "-r", "30", "-b:v", "256k", "-s", "320x240", "-f", "rtp", "-reorder_queue_size", "0", buffer, NULL);
    #endif

    fprintf(stdout, "video iniciado com pid=%d\n", video_pid);
  }
}

void video_stop() {
  if(video_pid != 0 && video_pid != -666) {
    fprintf(stdout, "finalizando processo %d\n", video_pid);
    kill(video_pid, SIGINT);
    video_pid = -666;
  }
}
