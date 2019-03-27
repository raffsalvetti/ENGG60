package br.com.arena64.testecapturamovimentocabeca;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

class TextViewTextChanger implements Runnable {

    private TextView tv;
    private String value;

    public TextViewTextChanger(TextView tv) {
        this.tv = tv;
    }

    public void updateText(String value){
        this.value = value;
    }

    @Override
    public void run() {
        tv.setText(value);
    }
}

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity implements SensorEventListener {
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

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mAagnetometer;
    private Sensor mRotationVector;
    private float[] mGravity;
    private float[] mGeomagnetic;
    private TextView mAzimut, mPitch, mRoll;
    private TextViewTextChanger mAzimutTextViewTextChanger, mPitchTextViewTextChanger, mRollTextViewTextChanger;
    private Button mButtonReset;
    private boolean calibrated = false;
    private float matrixR[] = new float[9];
    private float matrixI[] = new float[9];
    private float orientation[] = new float[3];
    private float orientationAdjust[] = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
//        mContentView = findViewById(R.id.fullscreen_content);
        mAzimut = (TextView) findViewById(R.id.azimut);
        mPitch = (TextView) findViewById(R.id.pitch);
        mRoll = (TextView) findViewById(R.id.roll);
        mButtonReset = (Button) findViewById(R.id.button_reset);

        mAzimutTextViewTextChanger = new TextViewTextChanger(mAzimut);
        mPitchTextViewTextChanger = new TextViewTextChanger(mPitch);
        mRollTextViewTextChanger = new TextViewTextChanger(mRoll);

        mButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrate();
            }
        });

        // Set up the user interaction to manually show or hide the system UI.
        mAzimut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mAagnetometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mRotationVector, SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i("SENSOR", "onAccuracyChanged --> sensor: "+sensor+"; accuracy: " + accuracy);
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            boolean success = false;
            success = SensorManager.getRotationMatrix(matrixR, matrixI, mGravity, mGeomagnetic);
            if (success) {
                SensorManager.getOrientation(matrixR, orientation);
                // orientation contains: azimut, pitch and roll
//                mAzimutTextViewTextChanger.updateText("Azimut: " + Math.toDegrees(orientation[0] + orientationAdjust[0]));
//                mPitchTextViewTextChanger.updateText("Pitch: " + Math.toDegrees(orientation[1] + orientationAdjust[1]));
//                mRollTextViewTextChanger.updateText("Roll: " + Math.toDegrees(orientation[2] + orientationAdjust[2]));
//                this.runOnUiThread(mAzimutTextViewTextChanger);
//                this.runOnUiThread(mPitchTextViewTextChanger);
//                this.runOnUiThread(mRollTextViewTextChanger);

//                Log.i("SENSOR", "onSensorChanged --> azimut: " + Math.toDegrees(orientation[0]));
//                Log.i("SENSOR", "onSensorChanged --> pitch: " + Math.toDegrees(orientation[1]));
//                Log.i("SENSOR", "onSensorChanged --> roll: " + Math.toDegrees(orientation[2]));
            }
        }
        if(event.sensor.getType() == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
            SensorManager.remapCoordinateSystem()
            mAzimutTextViewTextChanger.updateText("Azimut: " + Math.toDegrees(event.values[0]));
            mPitchTextViewTextChanger.updateText("Pitch: " + Math.toDegrees(event.values[1]));
            mRollTextViewTextChanger.updateText("Roll: " + Math.toDegrees(event.values[2]));
            this.runOnUiThread(mAzimutTextViewTextChanger);
            this.runOnUiThread(mPitchTextViewTextChanger);
            this.runOnUiThread(mRollTextViewTextChanger);

        }
    }

    private void calibrate() {
        int i;

        for(i = 0 ; i < orientation.length ; i++) {
            orientationAdjust[i] = -1 * orientation[i];
        }
        calibrated = true;
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
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
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
}
