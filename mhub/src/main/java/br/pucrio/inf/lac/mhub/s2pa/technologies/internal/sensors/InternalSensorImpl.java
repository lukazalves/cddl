package br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.Timer;
import java.util.TimerTask;

import br.pucrio.inf.lac.mhub.components.Time;
import br.pucrio.inf.lac.mhub.models.locals.SensorDataExtended;

/*
 * Device internal sensors that are managed by the system service Context.SENSOR_SERVICE.
 */
public class InternalSensorImpl implements InternalSensor, SensorEventListener, Comparable<InternalSensor> {

    private SensorManager sensorManager;
    private Sensor sensor;

    protected String sensorName;

    private InternalSensorListener listener;
    private int sensorType;
    private int delay;

    private long lastUpdate = Time.getInstance().getCurrentTimestamp();

    private float last_x, last_y, last_z, speed;

    private SpeedUpdateTimerTask speedUpdateTimerTask;

    private Timer timer = new Timer();

    private Context context;


    /*
     * Constructor that receives the sensor type (class listed in the sensor).
     * For example, Sensor.TYPE_ACCELEROMETER.
     * See other values in http://developer.android.com/reference/android/hardware/Sensor.html
     */
    public InternalSensorImpl(Context context, int sensorType) {
        sensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(sensorType);
        this.sensorType = sensorType;
        this.context = context;
    }

    /*
     * Start reading the sensor information by registering this class as a listener to the sensor
     * @see br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.InternalSensor#start()
     */
    @Override
    public synchronized void start() {
        if (delay == SensorManager.SENSOR_DELAY_FASTEST
                || delay == SensorManager.SENSOR_DELAY_GAME
                || delay == SensorManager.SENSOR_DELAY_NORMAL
                || delay == SensorManager.SENSOR_DELAY_UI) {
            sensorManager.registerListener(this, sensor, delay);
        } else {
            sensorManager.registerListener(this, sensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    /*
     * Stops reading the sensor information by unregistering this class as a listener on the sensor manager
     * @see br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.InternalSensor#stop()
     */
    @Override
    public synchronized void stop() {
        sensorManager.unregisterListener(this, sensor);

    }

    /*
     * Sets the listener (SensorPhone class)
     * @see br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.InternalSensor#setListener(br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.InternalSensorListener)
     */
    @Override
    public void setListener(InternalSensorListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // NA
    }

    /*
     * Callback method execued by the sensor to submit changes in its values to this class.
     * The data are sent in the event parameter.
     * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
     */
    @Override
    public synchronized void onSensorChanged(SensorEvent event) {

        Sensor sensor = event.sensor;

        SensorDataExtended sensorDataExtended = new SensorDataExtended();
        sensorDataExtended.setSensorName(event.sensor.getName());
        if (event.values != null && event.values.length > 0) {
            float[] floatValues = event.values;
            Double[] doubleValues = new Double[floatValues.length];
            for (int i = 0; i < floatValues.length; i++) {
                doubleValues[i] = Double.valueOf(floatValues[i]);
            }
            sensorDataExtended.setSensorValue(doubleValues);
            sensorDataExtended.setSensorObjectValue(doubleValues);
            sensorDataExtended.setSensorAccuracy(Double.valueOf(event.accuracy));

            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER || sensor.getType() == Sensor.TYPE_GYROSCOPE || sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED || sensor.getType() == Sensor.TYPE_GRAVITY || sensor.getType() == Sensor.TYPE_ROTATION_VECTOR || sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                sensorDataExtended.setAvailableAttributesList(new String[]{"X", "Y", "Z"});
            }

            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                if (speedUpdateTimerTask == null) {
                    speedUpdateTimerTask = new SpeedUpdateTimerTask();
                    long time = SpeedSensor.getInstance().getInterval();
                    timer.scheduleAtFixedRate(speedUpdateTimerTask, time, time);
                }

                long curTime = System.currentTimeMillis();
                // only allow one update every 100ms.
                //  if ((curTime - lastUpdate) > 1000) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;
                float x = floatValues[SensorManager.DATA_X];
                float y = floatValues[SensorManager.DATA_Y];
                float z = floatValues[SensorManager.DATA_Z];

                speed = Math.round(Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 1000);


                last_x = x;
                last_y = y;
                last_z = z;
                //   }


            } else if (sensorName != null && sensorName.endsWith("Location")) {
                sensorDataExtended.setAvailableAttributesList(new String[]{"Latitude", "Longitude", "Altitude", "Speed"});
            }

            if (listener != null) {
                listener.onInternalSensorChanged(sensorDataExtended);

            }
        }


    }

    /*
     * Return true if sensor exists on device, otherwise return false
     */
    public boolean exists() {
        return sensor != null;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    public String getName() {
        return sensor.getName();
    }

    public int getSensorType() {
        return sensorType;
    }

    @Override
    public int compareTo(InternalSensor sensor) {
        return getName().compareTo(sensor.getName());
    }

    private class SpeedUpdateTimerTask extends TimerTask {

        @Override
        public void run() {
            Double[] values = new Double[]{Double.valueOf(speed)};
            SensorDataExtended sensorDataExtended2 = new SensorDataExtended();
            sensorDataExtended2.setSensorName("Speed");
            sensorDataExtended2.setSensorObjectValue(values);

            if (listener != null) {
                listener.onInternalSensorChanged(sensorDataExtended2);
            }

        }
    }

    private String getMAC() {
        try {
            WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = manager.getConnectionInfo();
            String address = info.getMacAddress();
            if (address == null) address = "00:00:00:00:00:00";
            return address;
        } catch (Exception e) {
            //e.printStackTrace();
            return "00:00:00:00:00:00";
        }
    }

}
