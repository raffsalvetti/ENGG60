#!/bin/bash

#cvlc v4l2:///dev/video0 --sout '#transcode{vcodec=h264,vb=256,scale=1,acodec=mp3,ab=32,channels=2}:std{access=mmsh,mux=asfh,dst=:5544}'

cvlc v4l2:///dev/video0 --v4l2-width 320 --v4l2-height 240 --v4l2-chroma h264 --sout '#rtp{sdp=rtsp://:5544/}'
