package br.com.arena64.testecapturamovimentocabeca;

import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.MemoryFile;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class VlcSurfaceView extends SurfaceView implements IVLCVout.Callback {

    public interface Listener {
        void onCreateVideo();
        void onReleaseVideo();
    }

    public final static String TAG = "LibVLCAndroidSample/VideoActivity";

    // media player
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private Listener listener = null;

    public VlcSurfaceView(Context context) {
        super(context);
    }

    public VlcSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VlcSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public VlcSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSize(mVideoWidth, mVideoHeight);
    }

    public void addLisneter(Listener listener){
        this.listener = listener;
    }

    /*************
     * Surface
     *************/
    public void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        // get screen size
        int w = getWidth();
        int h = getHeight();

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        // force surface buffer size
        getHolder().setFixedSize(mVideoWidth, mVideoHeight);

        // set display size
        LayoutParams lp = getLayoutParams();
        lp.width = w;
        lp.height = h;
        setLayoutParams(lp);
        invalidate();
    }

    /*************
     * Player
     *************/

    public void createPlayer(String ip) {
        releasePlayer();
        try {
//            if (media.length() > 0) {
//                Toast toast = Toast.makeText(getContext(), media, Toast.LENGTH_LONG);
//                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
//                        0);
//                toast.show();
//            }

            // Create LibVLC
            // TODO: make this more robust, and sync with audio demo
            ArrayList<String> options = new ArrayList<String>();
            //options.add("--subsdec-encoding <encoding>");
//            options.add("--aout=opensles");
//            options.add("--audio-time-stretch"); // time stretching
//            options.add("-vvv"); // verbosity
            libvlc = new LibVLC(getContext());
            getHolder().setKeepScreenOn(true);

            // Create media player
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);

            // Set up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(this);
            //vout.setSubtitlesView(mSurfaceSubtitles);
            vout.addCallback(this);
            vout.attachViews();

            MemoryFile memoryFile = new MemoryFile("SDP", 1024 * 2);
            memoryFile.getOutputStream().write(("v=0\no=- 0 0 IN IP4 127.0.0.1\ns=No Name\nc=IN IP4 " + ip + "\nt=0 0\na=tool:libavformat 57.56.101\nm=video 8888 RTP/AVP 96\nb=AS:256\na=rtpmap:96 H264/90000\na=fmtp:96 packetization-mode=1").getBytes());
            FileDescriptor fileDescriptor = (FileDescriptor) MemoryFile.class.getMethod("getFileDescriptor").invoke(memoryFile);

            Media m = new Media(libvlc, fileDescriptor);
//            Media m = new Media(libvlc, Uri.parse(media));
            m.addOption(":fullscreen");
            mMediaPlayer.setMedia(m);
//            mMediaPlayer.setAspectRatio("16:9");
            mMediaPlayer.setScale(2f);
            mMediaPlayer.play();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error creating player!", Toast.LENGTH_LONG).show();
        }
    }

    // TODO: handle this cleaner
    public void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        libvlc.release();
        libvlc = null;

        mVideoWidth = 0;
        mVideoHeight = 0;
        listener.onReleaseVideo();
    }

    /*************
     * Events
     *************/

    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

//    @Override
//    public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
//        if (width * height == 0)
//            return;
//
//        // store video size
//        mVideoWidth = width;
//        mVideoHeight = height;
//        setSize(mVideoWidth, mVideoHeight);
//    }

    @Override
    public void onSurfacesCreated(IVLCVout vout) {
        Log.d(TAG, "onSurfacesCreated");
        if(listener != null)
            listener.onCreateVideo();
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {
        Log.d(TAG, "onSurfacesDestroyed");
        if(listener != null)
            listener.onReleaseVideo();
    }

    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<VlcSurfaceView> mOwner;

        public MyPlayerListener(VlcSurfaceView owner) {
            mOwner = new WeakReference<VlcSurfaceView>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            VlcSurfaceView player = mOwner.get();

            switch(event.type) {
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }

//    @Override
//    public void eventHardwareAccelerationError() {
//        // Handle errors with hardware acceleration
//        Log.e(TAG, "Error with hardware acceleration");
//        this.releasePlayer();
//        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
//    }
}