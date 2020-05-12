package br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors;

/**
 * Created by bertodetacio on 11/06/17.
 */

public class SpeedSensor implements InternalSensor {

    public static final String NAME = "Speed";

    public static final int ID = -3;

    private long interval = 1000;

    private boolean enable = false;

    private static SpeedSensor instance = null;

    private SpeedSensor() {
    }

    public static SpeedSensor getInstance() {
        if (instance == null) {
            instance = new SpeedSensor();
        }
        return instance;
    }

    @Override
    public void start() {
        enable = true;
    }

    @Override
    public void stop() {
        enable = false;
    }

    @Override
    public void setListener(InternalSensorListener listener) {

    }

    @Override
    public String getName() {
        return null;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }


}
