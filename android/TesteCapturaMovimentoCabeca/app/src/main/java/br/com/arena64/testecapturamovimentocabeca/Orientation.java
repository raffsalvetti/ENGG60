package br.com.arena64.testecapturamovimentocabeca;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.util.Arrays;

public class Orientation implements SensorEventListener {

    private static final String TAG = "Orientation";

    public interface Listener {
        void onOrientationChanged(int x, int y);
    }

    private static final int SENSOR_DELAY_MICROS = 30 * 1000; //em ms

    private final WindowManager mWindowManager;

    private final SensorManager mSensorManager;

    @Nullable
    private final Sensor mRotationSensor;

    private int mLastAccuracy;
    private Listener mListener;

    private boolean calibrate = false;
    private boolean calibrated = false;
    private float[] calibratedRotationMatrix = new float[9];

    public Orientation(Activity activity) {
        mWindowManager = activity.getWindow().getWindowManager();
        mSensorManager = (SensorManager) activity.getSystemService(Activity.SENSOR_SERVICE);

        // Can be null if the sensor hardware is not available
//        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
//        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_POSE_6DOF);
    }

    public void startListening(Listener listener) {
        if (mListener == listener) {
            return;
        }
        mListener = listener;
        if (mRotationSensor == null) {
            Log.w(TAG, "Rotation vector sensor not available; will not provide orientation data.");
            return;
        }
        mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY_MICROS);
    }

    public void stopListening() {
        mSensorManager.unregisterListener(this);
        mListener = null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (mLastAccuracy != accuracy) {
            mLastAccuracy = accuracy;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mListener == null) {
            return;
        }
        if (mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }
        if (event.sensor == mRotationSensor) {
            updateOrientation(event.values);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void updateOrientation(float[] rotationVector) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);

        final int worldAxisForDeviceAxisX;
        final int worldAxisForDeviceAxisY;

        // Remap the axes as if the device screen was the instrument panel,
        // and adjust the rotation matrix for the device orientation.
        switch (mWindowManager.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
            default:
//                Log.i(TAG, "updateOrientation: ROTATION_0");
                worldAxisForDeviceAxisX = SensorManager.AXIS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
                break;
            case Surface.ROTATION_90:
//                Log.i(TAG, "updateOrientation: ROTATION_90");
                worldAxisForDeviceAxisX = SensorManager.AXIS_Z;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
                break;
            case Surface.ROTATION_180:
//                Log.i(TAG, "updateOrientation: ROTATION_180");
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z;
                break;
            case Surface.ROTATION_270:
//                Log.i(TAG, "updateOrientation: ROTATION_270");
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z;
                worldAxisForDeviceAxisY = SensorManager.AXIS_X;
                break;
        }

        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX, worldAxisForDeviceAxisY, adjustedRotationMatrix);

        // Transform rotation matrix into azimuth/pitch/roll
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);

        if(calibrate) {
            Log.i(TAG, "Calibrando: " + Arrays.toString(adjustedRotationMatrix));
            calibratedRotationMatrix = adjustedRotationMatrix.clone();
            calibrate = false;
            calibrated = true;
        }

        if(calibrated) {
            SensorManager.getAngleChange(orientation, adjustedRotationMatrix, calibratedRotationMatrix);
        }


        // Convert radians to degrees
        int x = (int)Math.round(Math.toDegrees(orientation[0]));
        int y = (int)Math.round(Math.toDegrees(orientation[1]));

        x = 100 * (x + 90) / 180;
        y = 100 * (y + 90) / 180;

        mListener.onOrientationChanged(x, y);
    }

    public void calibrate(){
        calibrate = true;
    }
}
