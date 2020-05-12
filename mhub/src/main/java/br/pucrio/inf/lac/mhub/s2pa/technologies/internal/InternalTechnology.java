package br.pucrio.inf.lac.mhub.s2pa.technologies.internal;

import android.content.Context;

import br.pucrio.inf.lac.mhub.components.AppUtils;
import br.pucrio.inf.lac.mhub.s2pa.base.Technology;
import br.pucrio.inf.lac.mhub.s2pa.base.TechnologyListener;
import br.pucrio.inf.lac.mhub.s2pa.technologies.internal.devices.SensorPhone;

/*
 * This class represents the internal technology, a abstraction that manages all sensors of the device.
 * Like others technologies, it is called by S2PAService class to initialize the sensors and read values from them.
 * It uses the SensorPhone class as a proxy to the actual sensors classes that holds the specifics implementations
 * of connecting and reading the sensors data.
 */
public class InternalTechnology implements Technology {

    public final static int ID = 3;

    private static InternalTechnology instance;
    private Context ac;

    private static final String TAG = InternalTechnology.class.getName();

    private SensorPhone sensorPhone;
    private TechnologyListener listener;

    private boolean enable = false;

    private InternalTechnology(Context context) {
        this.ac = context;
    }

    public static InternalTechnology getInstance(Context context) {
        if (instance == null)
            instance = new InternalTechnology(context);
        return instance;
    }

    /*
     * The internal technology is always initialize (you cannot disconnect the
     * internal technology)
     *
     * @see br.pucrio.inf.lac.mhub.s2pa.base.Technology#initialize()
     */
    @Override
    public boolean initialize() {
        // TODO Auto-generated method stub
        return true;
    }

    /*
     * The enable step is when it is created the SensorPhone object to
     * communicate with the sensors
     *
     * @see br.pucrio.inf.lac.mhub.s2pa.base.Technology#enable()
     */
    @Override
    public void enable() {
        sensorPhone = SensorPhone.getInstance(ac);
        sensorPhone.enable();
        enable = true;
    }

    public boolean isEnable() {
        return enable;
    }

    /*
     * Sets the listener (the S2PAService class)
     *
     * @see
     * br.pucrio.inf.lac.mhub.s2pa.base.Technology#setListener(br.pucrio.inf
     * .lac.mhub.s2pa.base.TechnologyListener)
     */
    @Override
    public void setListener(TechnologyListener listener) {
        this.listener = listener;
        SensorPhone.getInstance(ac).setListener(listener);
    }

    /*
     * There is no need to scan for mobile objects since there is only one
     * object: the SensorPhone
     *
     * @see br.pucrio.inf.lac.mhub.s2pa.base.Technology#startScan(boolean)
     */
    @Override
    public void startScan(boolean autoconnect) {
        if (listener == null)
            AppUtils.logger('e', TAG, "NULL Listener");
        sensorPhone.scan();
    }

    /*
     * Similarly, There is no need to stop scan for mobile objects
     *
     * @see br.pucrio.inf.lac.mhub.s2pa.base.Technology#stopScan()
     */
    @Override
    public void stopScan() {
        // NA
    }

    @Override
    public void readSensorValue(String macAddress, String serviceName) {
        // NA
    }

    @Override
    public void writeSensorValue(String macAddress, String serviceName,
                                 Object value) {
        // NA
    }

    /*
     * Always connected
     *
     * @see
     * br.pucrio.inf.lac.mhub.s2pa.base.Technology#connect(java.lang.String)
     */
    @Override
    public boolean connect(String macAddress) {
        if (macAddress == null) {
            AppUtils.logger('w', TAG, "Connect: Unspecified address or black list");
            return false;
        }

        return true;

    }

    /*
     * Nothing to disconnect
     *
     * @see
     * br.pucrio.inf.lac.mhub.s2pa.base.Technology#disconnect(java.lang.String)
     */
    @Override
    public boolean disconnect(String macAddress) {
        if (macAddress == null) {
            AppUtils.logger('w', TAG, "Disconnect: Unspecified address");
            return false;
        }

        return true;
    }

    /*
     * Disable de reading of sensors values and clear resources
     *
     * @see br.pucrio.inf.lac.mhub.s2pa.base.Technology#destroy()
     */
    @Override
    public void destroy() {
        if (sensorPhone == null) return;
        sensorPhone.disable();
        sensorPhone = null;
    }

    @Override
    public void addToWhiteList(String macAddress) {

    }

    @Override
    public boolean removeFromWhiteList(String macAddress) {
        return false;
    }

    @Override
    public void clearWhiteList() {

    }

    @Override
    public void addToBlackList(String macAddress) {

    }

    @Override
    public boolean isInBlackList(String macAddress) {
        return false;
    }

    @Override
    public void clearBlackList() {

    }

    @Override
    public boolean isInWhiteList(String macAddress) {
        return false;
    }

    @Override
    public boolean removeFromBlackList(String macAddress) {
        return false;
    }


    public SensorPhone getSensorPhone() {
        return sensorPhone;
    }

    public void setSensorPhone(SensorPhone sensorPhone) {
        this.sensorPhone = sensorPhone;
    }
}
