package br.com.arena64.testecapturamovimentocabeca;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.JsonWriter;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

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
    private float[] mGravityVector;
    private float[] mGeomagneticVector;
    private TextView mAzimutCalculated, mPitchCalculated, mRollCalculated, mAzimutGiven, mPitchGiven, mRollGiven,
            mAccelerometerAccuracy, mMagnetometerAccuracy, mRotationVectorAccuracy;
    private TextViewTextChanger mAzimutCalculatedTextViewTextChanger, mPitchCalculatedTextViewTextChanger, mRollCalculatedTextViewTextChanger,
            mAzimutGivenTextViewTextChanger, mPitchGivenTextViewTextChanger, mRollGivenTextViewTextChanger,
            mAccelerometerAccuracyTextViewTextChanger, mMagnetometerAccuracyTextChanger, mRotationVectorAccuracyTextChanger;
    private Button mButtonReset, mButtonSend;
    private boolean calibrated = false;
    private float rotationMatrix[] = new float[9];
    private float resetRotationMatrix[] = new float[9];
    private float inclinationMatrix[] = new float[9];
    private float orientation[] = new float[3];
    private SmoothOrientation smoothOrientation = new SmoothOrientation(3, 5);
    private ConcurrentLinkedQueue buffer = new ConcurrentLinkedQueue();
    private NetWorks netWorks;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
//        mContentView = findViewById(R.id.fullscreen_content);

        mAzimutCalculated = (TextView) findViewById(R.id.azimutCalculated);
        mPitchCalculated = (TextView) findViewById(R.id.pitchCalculated);
        mRollCalculated = (TextView) findViewById(R.id.rollCalculated);

        mAzimutGiven = (TextView) findViewById(R.id.azimutGiven);
        mPitchGiven = (TextView) findViewById(R.id.pitchGiven);
        mRollGiven = (TextView) findViewById(R.id.rollGiven);

        mAccelerometerAccuracy = (TextView) findViewById(R.id.accelerometerAccuracy);
        mMagnetometerAccuracy = (TextView) findViewById(R.id.magnetometerAccuracy);
        mRotationVectorAccuracy = (TextView) findViewById(R.id.rotationVectorAccuracy);

        mButtonReset = (Button) findViewById(R.id.button_reset);
        mButtonSend = (Button) findViewById(R.id.button_send);

        mAzimutCalculatedTextViewTextChanger = new TextViewTextChanger(mAzimutCalculated);
        mPitchCalculatedTextViewTextChanger = new TextViewTextChanger(mPitchCalculated);
        mRollCalculatedTextViewTextChanger = new TextViewTextChanger(mRollCalculated);

        mAzimutGivenTextViewTextChanger = new TextViewTextChanger(mAzimutGiven);
        mPitchGivenTextViewTextChanger = new TextViewTextChanger(mPitchGiven);
        mRollGivenTextViewTextChanger = new TextViewTextChanger(mRollGiven);

        mAccelerometerAccuracyTextViewTextChanger = new TextViewTextChanger(mAccelerometerAccuracy);
        mMagnetometerAccuracyTextChanger = new TextViewTextChanger(mMagnetometerAccuracy);
        mRotationVectorAccuracyTextChanger = new TextViewTextChanger(mRotationVectorAccuracy);

        mButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrate();
            }
        });
        mButtonSend.setOnClickListener(new View.OnClickListener() {
                                                   @Override
                                                   public void onClick(View view) {
                                                       sendData();
                                                   }
                                               }
        );

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
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        try {
            netWorks = new NetWorks("192.168.43.1", 3200, buffer);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private void sendData() {
        if(netWorks.isActive()) {
            netWorks.stop();
        } else {
            new Thread(netWorks).start();
        }
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
        mSensorManager.registerListener(this, mAccelerometer, 16000);
        mSensorManager.registerListener(this, mAagnetometer, 16000);

//        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
//        mSensorManager.registerListener(this, mAagnetometer, SensorManager.SENSOR_DELAY_GAME);
//        mSensorManager.registerListener(this, mRotationVector, SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    private String resolveAccuracy(int accuracy) {
        switch (accuracy) {
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return "HIGH";
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                return "MEDIUM";
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return "LOW";
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                return "UNRELIABLE";
            default:
                return "WTF";
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mAccelerometerAccuracyTextViewTextChanger.updateText("AccelerometerAccuracy: " + resolveAccuracy(accuracy));
            this.runOnUiThread(mAccelerometerAccuracyTextViewTextChanger);
        }
        if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mMagnetometerAccuracyTextChanger.updateText("MagnetometerAccuracy: " + resolveAccuracy(accuracy));
            this.runOnUiThread(mMagnetometerAccuracyTextChanger);
        }
        if (sensor.getType() == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
            mRotationVectorAccuracyTextChanger.updateText("RotationVectorAccuracy: " + resolveAccuracy(accuracy));
            this.runOnUiThread(mRotationVectorAccuracyTextChanger);
        }
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
//            values[0]: Acceleration minus Gx on the x-axis
//            values[1]: Acceleration minus Gy on the y-axis
//            values[2]: Acceleration minus Gz on the z-axis
            mGravityVector = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
//            All values are in micro-Tesla (uT) and measure the ambient magnetic field in the X, Y and Z axis.
            mGeomagneticVector = event.values;
        if (mGravityVector != null && mGeomagneticVector != null) {
            boolean success = false;
            success = SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, mGravityVector, mGeomagneticVector);
            if (success) {
                if(!calibrated) {
                    SensorManager.getOrientation(rotationMatrix, orientation);
                } else {
                    SensorManager.getAngleChange(orientation, rotationMatrix, resetRotationMatrix);
                }

//                 orientation contains: azimut, pitch and roll
                mAzimutCalculatedTextViewTextChanger.updateText("Azimut: " + String.format("%.2f", Math.toDegrees(orientation[0])));
                mPitchCalculatedTextViewTextChanger.updateText("Pitch: " + String.format("%.2f", Math.toDegrees(orientation[1])));
                mRollCalculatedTextViewTextChanger.updateText("Roll: " + String.format("%.2f", Math.toDegrees(orientation[2])));
                this.runOnUiThread(mAzimutCalculatedTextViewTextChanger);
                this.runOnUiThread(mPitchCalculatedTextViewTextChanger);
                this.runOnUiThread(mRollCalculatedTextViewTextChanger);

                smoothOrientation.addSample(orientation);
                float[] average = smoothOrientation.average();
//                float[] average = orientation;

                buffer.add(gson.toJson(new double[] {
                        Math.round(Math.toDegrees(average[0])),
                        Math.round(Math.toDegrees(average[1])),
                        Math.round(Math.toDegrees(average[2]))
                }));

                mAzimutGivenTextViewTextChanger.updateText("Azimut: " + String.format("%.2f", Math.toDegrees(average[0])));
                mPitchGivenTextViewTextChanger.updateText("Pitch: " + String.format("%.2f", Math.toDegrees(average[1])));
                mRollGivenTextViewTextChanger.updateText("Roll: " + String.format("%.2f", Math.toDegrees(average[2])));

                this.runOnUiThread(mAzimutGivenTextViewTextChanger);
                this.runOnUiThread(mPitchGivenTextViewTextChanger);
                this.runOnUiThread(mRollGivenTextViewTextChanger);

            }
        }
        if(event.sensor.getType() == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
//            float[] tmpOrientaion = Arrays.copyOf(event.values, 3);
//            float[] tmpRotationMatrix = new float[9];
//            SensorManager.getRotationMatrixFromVector(tmpRotationMatrix, tmpOrientaion);
//            if(calibrated) {
//                SensorManager.getAngleChange(tmpOrientaion, tmpRotationMatrix, resetRotationMatrix);
//            }
////            Log.i("TESTE", "onSensorChanged: " + event.values.length);
//            mAzimutGivenTextViewTextChanger.updateText("Azimut: " + String.format("%.2f", Math.toDegrees(tmpOrientaion[0])));
//            mPitchGivenTextViewTextChanger.updateText("Pitch: " + String.format("%.2f", Math.toDegrees(tmpOrientaion[1])));
//            mRollGivenTextViewTextChanger.updateText("Roll: " + String.format("%.2f", Math.toDegrees(tmpOrientaion[2])));
//
////            mRotationVectorAccuracyTextChanger.updateText("RotationVectorAccuracy: " + String.format("%.2f", Math.toDegrees(event.values[3])));
////            this.runOnUiThread(mRotationVectorAccuracyTextChanger);
//
//            this.runOnUiThread(mAzimutGivenTextViewTextChanger);
//            this.runOnUiThread(mPitchGivenTextViewTextChanger);
//            this.runOnUiThread(mRollGivenTextViewTextChanger);
        }
    }

    private void calibrate() {
        resetRotationMatrix = Arrays.copyOf(rotationMatrix, rotationMatrix.length);
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // captura de eventos do joystick
        if(event.getSource() == InputDevice.SOURCE_GAMEPAD || event.getSource() == InputDevice.SOURCE_DPAD || event.getSource() == InputDevice.SOURCE_JOYSTICK) {

        }

        return super.dispatchKeyEvent(event);
    }
}
