package br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;

import br.pucrio.inf.lac.mhub.models.locals.SensorDataExtended;

/*
 * Battery sensor. Uses an asynchronous task to read the level of the battery
 * from time to time (informed by the constructor's parameter readInterval)
 */
public class BatterySensor implements InternalSensor {

    public static final String NAME = "Battery";
    private InternalSensorListener listener;  // Listener pointing to SensorPhone class
    private Context context;

    public static final int ID = -2;

    // Background task to view the level of the battery
    private AsyncTask<InternalSensorListener, SensorDataExtended, Void> task;

    private long readInterval;  // battery level reading interval, in milliseconds

    public BatterySensor(Context context, long readInterval) {
        this.context = context;
        this.readInterval = readInterval;
    }

    /*
     * Starts battery level reading task
     * @see br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.InternalSensor#start()
     */
    @Override
    public void start() {
        if (task == null) {
            task = new BatterySensorTask().execute(listener);
        }
    }

    /*
     * Stops battery level reading task
     * @see br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.InternalSensor#stop()
     */
    @Override
    public void stop() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
    }

    /*
     * Sets Sets the listener (SensorPhone class)
     * @see br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.InternalSensor#setListener(br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.InternalSensorListener)
     */
    @Override
    public void setListener(InternalSensorListener listener) {
        this.listener = listener;
    }

    /*
     * Battery level reading task. Runs in the background every interval (READ_INTERVAL)
     */
    private class BatterySensorTask extends
            AsyncTask<InternalSensorListener, SensorDataExtended, Void> {

        InternalSensorListener listener;

        /*
         * Read the battery level, sends the data and then sleeps for READ_INTERVAL milliseconds.
         * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
         */
        @Override
        protected Void doInBackground(InternalSensorListener... params) {

            this.listener = params[0];

            while (true) {
                if (isCancelled())
                    break;
                IntentFilter battFilter = new IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED);
                Intent iBatt = context.getApplicationContext()
                        .registerReceiver(null, battFilter);
                int level = iBatt.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                float scale = iBatt.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int battPercent = (int) ((level / scale) * 100);

                SensorDataExtended data = new SensorDataExtended();

                data.setSensorName(NAME);
                Double[] values = new Double[]{Double.valueOf(battPercent)};
                data.setSensorObjectValue(values);
                publishProgress(data);
                try {
                    Thread.sleep(readInterval);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
            return null;

        }

        /*
         * Sends the read data to the listener
         * @see android.os.AsyncTask#onProgressUpdate(java.lang.Object[])
         */
        @Override
        protected void onProgressUpdate(SensorDataExtended... sensorDataExtendeds) {
            listener.onInternalSensorChanged(sensorDataExtendeds[0]);
        }

    }

    @Override
    public String getName() {
        return NAME;
    }
}
