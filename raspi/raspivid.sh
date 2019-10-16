#!/bin/bash
raspivid -o - -t 0 -n -w 320 -h 240 -fps 12 | cvlc -vvv stream:///dev/stdin --sout '#rtp{sdp=rtsp://:8554/}' :demux=h264
