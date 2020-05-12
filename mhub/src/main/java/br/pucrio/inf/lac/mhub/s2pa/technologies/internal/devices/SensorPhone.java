package br.pucrio.inf.lac.mhub.s2pa.technologies.internal.devices;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import br.pucrio.inf.lac.mhub.components.AppUtils;
import br.pucrio.inf.lac.mhub.components.MOUUID;
import br.pucrio.inf.lac.mhub.models.locals.SensorData;
import br.pucrio.inf.lac.mhub.models.locals.StartAllSensorsMessage;
import br.pucrio.inf.lac.mhub.models.locals.StartBatterySensorMessage;
import br.pucrio.inf.lac.mhub.models.locals.StartLocationSensorMessage;
import br.pucrio.inf.lac.mhub.models.locals.StartSensorMessageById;
import br.pucrio.inf.lac.mhub.models.locals.StartSensorMessageBySimpleName;
import br.pucrio.inf.lac.mhub.models.locals.StopAllSensorsMessage;
import br.pucrio.inf.lac.mhub.models.locals.StopBatterySensorMessage;
import br.pucrio.inf.lac.mhub.models.locals.StopLocationSensorMessage;
import br.pucrio.inf.lac.mhub.models.locals.StopSensorMessageById;
import br.pucrio.inf.lac.mhub.models.locals.StopSensorMessageBySimpleName;
import br.pucrio.inf.lac.mhub.s2pa.base.TechnologyListener;
import br.pucrio.inf.lac.mhub.s2pa.base.TechnologyListenerExtended;
import br.pucrio.inf.lac.mhub.s2pa.filter.S2PAFilter;
import br.pucrio.inf.lac.mhub.s2pa.technologies.internal.InternalTechnology;
import br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.BatterySensor;
import br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.InternalSensor;
import br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.InternalSensorImpl;
import br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.InternalSensorListener;
import br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.LocationSensor;
import br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.SpeedSensor;
import br.pucrio.inf.lac.mhub.util.MHubEventBus;

/*
 * SensorPhone represents the device containing the M-Hub (usually a cellphone).
 * It uses a list of sensors for which registers itself as a listener and receives data.
 * This class is used by InternalTechnology class to maintain a connection to the device's sensors.
 */
public class SensorPhone implements InternalSensorListener {

    public static final String DEVICE_NAME = "SensorPhone";

    private MOUUID device = null;

    // LCMUNIZ - atualizar intervalo e verificar se nao pode ser configuravel
    private static final long BATTERY_READ_INTERVAL = 1000; // battery level reading

    private static final long LOCATION_READ_INTERVAL = 1000; // battery level reading
    // interval, in
    // milliseconds

    private Double mRssi = 0.0;

    private String macAddress;  // This MAC address by convention represents the device

    private MOUUID mouuid;

    private TechnologyListener listener;

    private LocationSensor locationSensor;

    private BatterySensor batterySensor;

    private SpeedSensor speedSensor;

    public List<InternalSensor> sensors;

    private Context context;

    private static SensorPhone instance = null;

    /*
     * Constructor. Receives the listener (the S2PAService) and the ID of the
     * technology Calls startAllSensors to initialize the internal sensors of the
     * device
     */
    private SensorPhone(Context context) {

        init(context);

    }

    private void init(Context context) {
        this.context = context;
        macAddress = getMAC();
        mouuid = new MOUUID(InternalTechnology.ID, macAddress);

        sensors = new ArrayList<InternalSensor>();
        // startAllSensors();

        //listener.onMObjectConnected(mouuid);

        if (!MHubEventBus.getDefault().isRegistered(this)) {
            MHubEventBus.getDefault().register(this);
        }

    }

    public static SensorPhone getInstance(Context context) {
        if (instance == null) {
            instance = new SensorPhone(context);
        }
        return instance;
    }

    public void scan() {

    }

    /*
     * Adds the internal sensor of the device and sets this class as the
     * listener for changes on sensor's values
     */
    private synchronized void startAllSensors(int delayType) {

        List<String> listAvailableServices = new ArrayList<>();
        listener.onMObjectConnected(mouuid);


        if (locationSensor == null) {
            locationSensor = new LocationSensor(context, LOCATION_READ_INTERVAL);
            sensors.add(locationSensor);
            listAvailableServices.add(LocationSensor.NAME);
        }

        if (batterySensor == null) {
            batterySensor = new BatterySensor(context, BATTERY_READ_INTERVAL);
            sensors.add(batterySensor);
            listAvailableServices.add(BatterySensor.NAME);
        }

        if (speedSensor == null) {
            speedSensor = SpeedSensor.getInstance();
            sensors.add(speedSensor);
            listAvailableServices.add(SpeedSensor.NAME);
        }


        SensorManager mSensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> sensList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensList) {
            int idType = sensor.getType();
            listAvailableServices.add(sensor.getName());
            addSensor(idType, delayType);

        }

        for (InternalSensor internalSensor : sensors) {
            internalSensor.setListener(this);
            internalSensor.start();
        }
        listener.onMObjectServicesDiscovered(mouuid, listAvailableServices);
    }


    private synchronized void stopAllSensors() {

        if (locationSensor != null) {
            sensors.remove(locationSensor);
            locationSensor.stop();
            locationSensor = null;
        }


        if (batterySensor != null) {
            sensors.remove(batterySensor);
            batterySensor.stop();
            batterySensor = null;
        }


        if (speedSensor != null) {
            sensors.remove(speedSensor);
            speedSensor.stop();
            speedSensor = null;
        }

        SensorManager mSensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);

        ArrayList<InternalSensor> sensorsRemove = new ArrayList<InternalSensor>();

        for (InternalSensor internalSensor : sensors) {
            internalSensor.stop();
            sensorsRemove.add(internalSensor);
        }

        sensors.removeAll(sensorsRemove);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public synchronized void onMessageEvent(StartLocationSensorMessage startLocationSensorMessage) {


        long interval = startLocationSensorMessage.getInterval();

        List<String> listAvailableServices = new ArrayList<>();
        listener.onMObjectConnected(mouuid);

        if (locationSensor == null) {
            locationSensor = new LocationSensor(context, interval);
            sensors.add(locationSensor);
            listAvailableServices.add(LocationSensor.NAME);
            locationSensor.setListener(this);
            locationSensor.start();
            MHubEventBus.getDefault().getHistory().remove(startLocationSensorMessage);
        }


        listener.onMObjectServicesDiscovered(mouuid, listAvailableServices);

    }

    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public synchronized void onMessageEvent(StopLocationSensorMessage stopLocationSensorMessage) {
        if (locationSensor != null) {
            sensors.remove(locationSensor);
            locationSensor.stop();
            locationSensor = null;
            MHubEventBus.getDefault().getHistory().remove(stopLocationSensorMessage);
        }
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public synchronized void onMessageEvent(StartBatterySensorMessage startBatterySensorMessage) {

        List<String> listAvailableServices = new ArrayList<>();

        listener.onMObjectConnected(mouuid);

        long interval = startBatterySensorMessage.getInterval();
        if (batterySensor == null) {
            batterySensor = new BatterySensor(context, interval);
            batterySensor.setListener(this);
            sensors.add(batterySensor);
            batterySensor.setListener(this);
            batterySensor.start();
            listAvailableServices.add(batterySensor.getName());
            MHubEventBus.getDefault().getHistory().remove(startBatterySensorMessage);
        }


        listener.onMObjectServicesDiscovered(mouuid, listAvailableServices);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public synchronized void onMessageEvent(StopBatterySensorMessage stopBatterySensorMessage) {
        if (batterySensor != null) {
            sensors.remove(batterySensor);
            batterySensor.stop();
            batterySensor = null;
            MHubEventBus.getDefault().getHistory().remove(stopBatterySensorMessage);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public synchronized void onMessageEvent(StartAllSensorsMessage startAllSensorsMessage) {
        int delayType = startAllSensorsMessage.getRate();
        startAllSensors(delayType);
        MHubEventBus.getDefault().getHistory().remove(startAllSensorsMessage);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public synchronized void onMessageEvent(StopAllSensorsMessage stopAllSensorsMessage) {
        stopAllSensors();
        MHubEventBus.getDefault().getHistory().remove(stopAllSensorsMessage);
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public synchronized void onMessageEvent(StartSensorMessageBySimpleName startSensorMessageBySimpleName) {

        String name = startSensorMessageBySimpleName.getName();

        Integer rate = startSensorMessageBySimpleName.getRate();

        List<String> listAvailableServices = new ArrayList<>();

        if (locationSensor == null && name.equalsIgnoreCase(LocationSensor.NAME)) {
            locationSensor = new LocationSensor(context, LOCATION_READ_INTERVAL);
            sensors.add(locationSensor);
            listAvailableServices.add(LocationSensor.NAME);
            locationSensor.setListener(this);
            locationSensor.start();

        } else if (batterySensor == null && name.equalsIgnoreCase(BatterySensor.NAME)) {
            batterySensor = new BatterySensor(context, BATTERY_READ_INTERVAL);
            sensors.add(batterySensor);
            listAvailableServices.add(BatterySensor.NAME);
            batterySensor.setListener(this);
            batterySensor.start();
        } else {

            SensorManager mSensorManager = (SensorManager) context
                    .getSystemService(Context.SENSOR_SERVICE);

            List<Sensor> sensList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor sensor : sensList) {
                AppUtils.logger('i', getClass().getSimpleName(), "Nome do Sensor " + sensor.getName());


                if ((name.equalsIgnoreCase(SpeedSensor.NAME) && sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {
                    speedSensor = SpeedSensor.getInstance();
                    sensors.add(speedSensor);
                    listAvailableServices.add(SpeedSensor.NAME);
                    speedSensor.setListener(this);
                    speedSensor.start();
                    int idType = sensor.getType();
                    addSensor(idType, rate);
                    speedSensor.start();
                } else if (name.equalsIgnoreCase(sensor.getName())) {
                    listAvailableServices.add(sensor.getName());
                    int idType = sensor.getType();
                    addSensor(idType, rate);
                }

            }

            for (InternalSensor internalSensor : sensors) {
                internalSensor.setListener(this);
                internalSensor.start();

            }
        }
        listener.onMObjectConnected(mouuid);
        listener.onMObjectServicesDiscovered(mouuid, listAvailableServices);
        MHubEventBus.getDefault().getHistory().remove(startSensorMessageBySimpleName);

    }


    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public synchronized void onMessageEvent(StopSensorMessageBySimpleName stopSensorMessageBySimpleName) {

        String name = stopSensorMessageBySimpleName.getName();

        if (name.equalsIgnoreCase(LocationSensor.NAME)) {
            if (locationSensor != null) {
                sensors.remove(locationSensor);
                locationSensor.stop();
                locationSensor = null;
            }

        } else if (name.equalsIgnoreCase(BatterySensor.NAME)) {
            if (batterySensor != null) {
                sensors.remove(batterySensor);
                batterySensor.stop();
                batterySensor = null;

            }

        } else {


            ArrayList<InternalSensor> sensorsRemove = new ArrayList<InternalSensor>();


            for (InternalSensor internalSensor : sensors) {
                if (internalSensor.getName().equalsIgnoreCase(name)) {
                    internalSensor.stop();
                    sensorsRemove.add(internalSensor);
                }
            }

            sensors.removeAll(sensorsRemove);

        }
        MHubEventBus.getDefault().getHistory().remove(stopSensorMessageBySimpleName);

    }


    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public synchronized void onMessageEvent(StartSensorMessageById startSensorMessageById) {

        int id = startSensorMessageById.getId();

        Integer rate = startSensorMessageById.getRate();

        List<String> listAvailableServices = new ArrayList<>();


        if (id == LocationSensor.ID) {
            if (locationSensor == null) {
                locationSensor = new LocationSensor(context, LOCATION_READ_INTERVAL);
                locationSensor.setListener(this);
                sensors.add(locationSensor);
                listAvailableServices.add(LocationSensor.NAME);
                batterySensor.setListener(this);
                locationSensor.start();
                S2PAFilter.getInstance().enableSensor(LocationSensor.NAME);
            }
        } else if (id == BatterySensor.ID) {
            if (batterySensor == null) {
                batterySensor = new BatterySensor(context, BATTERY_READ_INTERVAL);
                batterySensor.setListener(this);
                sensors.add(batterySensor);
                listAvailableServices.add(BatterySensor.NAME);
                batterySensor.setListener(this);
                batterySensor.start();
                S2PAFilter.getInstance().enableSensor(BatterySensor.NAME);
            }
        } else {

            SensorManager mSensorManager = (SensorManager) context
                    .getSystemService(Context.SENSOR_SERVICE);

            List<Sensor> sensList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor sensor : sensList) {


                if ((id == SpeedSensor.ID && sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {
                    speedSensor = SpeedSensor.getInstance();
                    sensors.add(speedSensor);
                    listAvailableServices.add(SpeedSensor.NAME);
                    speedSensor.setListener(this);
                    speedSensor.start();
                    int idType = sensor.getType();
                    addSensor(idType, rate);
                    speedSensor.start();
                    S2PAFilter.getInstance().enableSensor(SpeedSensor.NAME);
                } else if (id == sensor.getType()) {
                    listAvailableServices.add(sensor.getName());
                    int idType = sensor.getType();
                    addSensor(idType, rate);
                    S2PAFilter.getInstance().enableSensor(sensor.getName());
                }

            }

            for (InternalSensor internalSensor : sensors) {
                internalSensor.setListener(this);
                internalSensor.start();

            }

        }

        listener.onMObjectConnected(mouuid);
        listener.onMObjectServicesDiscovered(mouuid, listAvailableServices);
        MHubEventBus.getDefault().getHistory().remove(startSensorMessageById);

    }


    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public synchronized void onMessageEvent(StopSensorMessageById stopSensorMessageById) {

        int id = stopSensorMessageById.getId();

        String name = null;

        if (id == LocationSensor.ID) {
            if (locationSensor != null) {
                sensors.remove(locationSensor);
                locationSensor.stop();
                locationSensor = null;
                S2PAFilter.getInstance().disableSensor(LocationSensor.NAME);
            }

        } else if (id == BatterySensor.ID) {
            if (batterySensor != null) {
                sensors.remove(batterySensor);
                batterySensor.stop();
                batterySensor = null;
                S2PAFilter.getInstance().disableSensor(BatterySensor.NAME);

            }

        } else {

            SensorManager mSensorManager = (SensorManager) context
                    .getSystemService(Context.SENSOR_SERVICE);


            ArrayList<InternalSensor> sensorsRemove = new ArrayList<InternalSensor>();
            List<Sensor> sensList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor sensor : sensList) {

                if (id == sensor.getType()) {

                    name = sensor.getName();

                }
            }

            if (name != null) {
                for (InternalSensor internalSensor : sensors) {

                    if (internalSensor.getName().equalsIgnoreCase(name)) {
                        S2PAFilter.getInstance().disableSensor(internalSensor.getName());
                        internalSensor.stop();
                        sensorsRemove.add(internalSensor);
                    }
                }

                sensors.removeAll(sensorsRemove);
            }

        }
        MHubEventBus.getDefault().getHistory().remove(stopSensorMessageById);

    }


    /*

     * Adds sensor to list of sensors if it exists
     */
    private synchronized void addSensor(int sensorType, Integer delay) {
        InternalSensorImpl is = new InternalSensorImpl(context, sensorType);

        if (delay != null) {
            is.setDelay(delay);
        }
        if (is.exists()) {
            if (!sensors.contains(is)) {
                sensors.add(is);
            }

        } else {
            is = null;
        }
    }


    /*
     * Inform the service that the device was found and connected Stars all the
     * sensors
     */
    public synchronized void enable() {
        if (listener != null) {
            listener.onMObjectFound(mouuid, mRssi);
            listener.onMObjectConnected(mouuid);
            //startSensors();
        }
    }

    /*
     * Stops all sensors Inform the service that the object was disconnected
     */
    public synchronized void disable() {
        stopSensors();
        List<String> services = new ArrayList<>();

        if (listener != null) {

            listener.onMObjectDisconnected(mouuid, services);
        }
    }

    /*
     * Callback method to receive updates on sensor's data Inform the service
     * the change and pass others informations
     */
    @Override
    public synchronized void onInternalSensorChanged(SensorData sensorData) {

        sensorData.setMouuid(mouuid.toString());
        sensorData.setSignal(mRssi);

        if (listener != null) {

            if (listener instanceof TechnologyListenerExtended) {
                TechnologyListenerExtended technologyListenerExtended = (TechnologyListenerExtended) listener;
                technologyListenerExtended.onMObjectValueRead(mouuid, sensorData);
            } else {
                listener.onMObjectValueRead(mouuid, mRssi, sensorData.getSensorName(), sensorData.getSensorValue());
            }
        }
    }

    /*
     * Starts all sensors;
     */
    private synchronized void startSensors() {
        for (InternalSensor internalSensor : sensors) {
            internalSensor.start();
        }
    }

    /*
     * Stops all sensors
     */
    private synchronized void stopSensors() {
        List<String> list = new ArrayList<>();
        for (InternalSensor internalSensor : sensors) {
            internalSensor.stop();
            list.add(internalSensor.getName());
        }
        if (listener != null) {
            listener.onMObjectDisconnected(mouuid, list);
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

    public TechnologyListener getListener() {
        return listener;
    }

    public void setListener(TechnologyListener listener) {
        this.listener = listener;
    }

}
