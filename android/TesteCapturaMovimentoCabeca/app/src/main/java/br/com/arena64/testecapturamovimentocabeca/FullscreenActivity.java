package br.com.arena64.testecapturamovimentocabeca;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

class MeanFilter extends ArrayList<Float> {
    private int windowSize;

    public MeanFilter(int windowSize) {
        this.windowSize = windowSize;
    }

    public void addSample(Float sample) {
        if(size() >= this.windowSize)
            remove(0);
        add(sample);
    }

    public float average() {
        float sum = 0;
        for(Float i : this) {
            sum += i.floatValue();
        }
        return sum / size();
    }
}

class SmoothOrientation {
    private MeanFilter[] buffer;

    public SmoothOrientation(int dimension, int windowSize) {
        buffer = new MeanFilter[dimension];
        for(int i = 0 ; i < buffer.length ; i++) {
            buffer[i] = new MeanFilter(windowSize);
        }
    }

    public void addSample(float[] samples) {
        for(int i = 0 ; i < buffer.length ; i ++ ) {
            buffer[i].addSample(samples[i]);
        }
    }

    public float[] average() {
        float[] av = new float[buffer.length];
        for(int i = 0 ; i < buffer.length; i++) {
            av[i] = buffer[i].average();
        }
        return av;
    }

}

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity implements Orientation.Listener, NetWorks.Listener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
//            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
//                    | View.SYSTEM_UI_FLAG_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private static final String TAG = "FullscreenActivity";
    private Button mButtonReset;
    private Switch aSwitchSendData;
    private TextView textViewConectado, textViewStatus, textViewCoordenadas;
    private Orientation mOrientation;
    private NetWorks netWorks;
    private VlcSurfaceView vlcSurfaceView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


                setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
//        mContentView = findViewById(R.id.fullscreen_content);

        mButtonReset = findViewById(R.id.button_calibrar_celular);
        mButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrate();
            }
        });

        aSwitchSendData = findViewById(R.id.switchSendCoordinates);
        aSwitchSendData.setClickable(false);
        aSwitchSendData.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!isChecked)
                    textViewStatus.setText(String.format("Sem enviar coordenadas."));
            }
        });

        textViewConectado = findViewById(R.id.textView_conctado);
        textViewStatus = findViewById(R.id.textView_status);
        textViewCoordenadas = findViewById(R.id.textView_coordenadas);

        // Set up the user interaction to manually show or hide the system UI.
//        mAzimutCalculated.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                toggle();
//            }
//        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
//        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

//        String uri = "rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mov";
         String uri = "rtsp://192.168.25.20:8888/live.sdp";
        vlcSurfaceView = findViewById(R.id.vlc_surface_view);


        mOrientation = new Orientation(this);
        netWorks = new NetWorks(true, this);
        netWorks.startBeacon();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mOrientation.startListening(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mOrientation.stopListening();

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    protected void onResume() {
        super.onResume();
//        vlcSurfaceView.setSize(800, 600);
        vlcSurfaceView.createPlayer("rtsp://192.168.25.20:8888/live.sdp");
    }

    protected void onPause() {
        super.onPause();
//        mSensorManager.unregisterListener(this);
        vlcSurfaceView.releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        vlcSurfaceView.releasePlayer();
    }

    private void calibrate() {
        mOrientation.calibrate();
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // captura de eventos do joystick
        if(event.getSource() == InputDevice.SOURCE_GAMEPAD || event.getSource() == InputDevice.SOURCE_DPAD || event.getSource() == InputDevice.SOURCE_JOYSTICK) {

        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onOrientationChanged(final int x, final int y) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewCoordenadas.setText(String.format("%d,%d", x, y));
            }
        });
        if(aSwitchSendData.isChecked()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textViewStatus.setText(String.format("Enviando coordenadas: %d:%d", x, y));
                }
            });
            netWorks.sendMessage(x + ";" + y);
        }
    }

    @Override
    public void onConnect(final String ipAddress) {
        Log.i(TAG, "Conectado em : " + ipAddress);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                aSwitchSendData.setClickable(true);
                textViewConectado.setText(String.format("Conectado em: %s", ipAddress));
                textViewStatus.setText(String.format("Conectado no controlador %s", ipAddress));
            }
        });
    }

    @Override
    public void onDisconnect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                aSwitchSendData.setChecked(false);
                aSwitchSendData.setClickable(false);
                textViewConectado.setText(R.string.conectado_em);
                textViewStatus.setText(String.format("Desconectado do controlador!"));
            }
        });
    }

    @Override
    public void onBeacon() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewStatus.setText(String.format("Localizando controlador..."));
            }
        });
    }
}
