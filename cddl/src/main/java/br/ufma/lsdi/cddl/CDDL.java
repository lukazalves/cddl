package br.ufma.lsdi.cddl;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import java.util.List;

import br.pucrio.inf.lac.mhub.components.AppUtils;
import br.pucrio.inf.lac.mhub.models.locals.S2PAStartedMessage;
import br.pucrio.inf.lac.mhub.models.locals.StartAllSensorsMessage;
import br.pucrio.inf.lac.mhub.models.locals.StartBatterySensorMessage;
import br.pucrio.inf.lac.mhub.models.locals.StartCommunicationTechnologyMessage;
import br.pucrio.inf.lac.mhub.models.locals.StartLocationSensorMessage;
import br.pucrio.inf.lac.mhub.models.locals.StartSensorMessageById;
import br.pucrio.inf.lac.mhub.models.locals.StartSensorMessageBySimpleName;
import br.pucrio.inf.lac.mhub.models.locals.StopAllSensorsMessage;
import br.pucrio.inf.lac.mhub.models.locals.StopBatterySensorMessage;
import br.pucrio.inf.lac.mhub.models.locals.StopCommunicationTechnologyMessage;
import br.pucrio.inf.lac.mhub.models.locals.StopLocationSensorMessage;
import br.pucrio.inf.lac.mhub.models.locals.StopSensorMessageById;
import br.pucrio.inf.lac.mhub.models.locals.StopSensorMessageBySimpleName;
import br.pucrio.inf.lac.mhub.s2pa.filter.S2PAFilter;
import br.pucrio.inf.lac.mhub.s2pa.technologies.ble.BLETechnology;
import br.pucrio.inf.lac.mhub.s2pa.technologies.bt.BTTechnology;
import br.pucrio.inf.lac.mhub.s2pa.technologies.internal.InternalTechnology;
import br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.BatterySensor;
import br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors.LocationSensor;
import br.pucrio.inf.lac.mhub.services.AdaptationService;
import br.pucrio.inf.lac.mhub.services.LocationService;
import br.pucrio.inf.lac.mhub.services.S2PAService;
import br.pucrio.inf.lac.mhub.util.MHubEventBus;
import br.ufma.lsdi.cddl.network.MicroBroker;
import br.ufma.lsdi.cddl.pubsub.CDDLFilterImpl;
import br.ufma.lsdi.cddl.qos.AbstractQoS;
import br.ufma.lsdi.cddl.qos.DeadlineQoS;
import br.ufma.lsdi.cddl.qos.DestinationOrderQoS;
import br.ufma.lsdi.cddl.qos.DurabilityQoS;
import br.ufma.lsdi.cddl.qos.History;
import br.ufma.lsdi.cddl.qos.HistoryQoS;
import br.ufma.lsdi.cddl.qos.LatencyBudgetQoS;
import br.ufma.lsdi.cddl.qos.LifespanQoS;
import br.ufma.lsdi.cddl.qos.LivelinessQoS;
import br.ufma.lsdi.cddl.qos.ReliabilityQoS;
import br.ufma.lsdi.cddl.qos.TimeBasedFilterQoS;
import br.ufma.lsdi.cddl.services.CommandService;
import br.ufma.lsdi.cddl.services.LocalDirectoryService;
import br.ufma.lsdi.cddl.services.QoCEvaluatorService;
import br.ufma.lsdi.cddl.util.Asserts;
import br.ufma.lsdi.cddl.util.CDDLEventBus;
import lombok.val;

/**
 * Created by lcmuniz on 05/05/17.
 */
public final class CDDL {

    public static final int SENSOR_DELAY_NORMAL = SensorManager.SENSOR_DELAY_NORMAL;

    public static final int SENSOR_DELAY_FASTEST = SensorManager.SENSOR_DELAY_FASTEST;

    public static final int SENSOR_DELAY_GAME = SensorManager.SENSOR_DELAY_GAME;

    public static final int SENSOR_DELAY_UI = SensorManager.SENSOR_DELAY_UI;

    public static final int INTERNAL_TECHNOLOGY_ID = InternalTechnology.ID;

    public static final int BLE_TECHNOLOGY_ID = BLETechnology.ID;

    public static final int BT_TECHNOLOGY_ID = BTTechnology.ID;

    private static final CDDL instance = new CDDL();

    private final S2PAFilter s2PAFilter = S2PAFilter.getInstance();

    private boolean servicesStarted = false;

    private S2PAStartedMessage s2PAStartedMessage;

    private Connection connection;

    private Context context;

    private CDDL() {
        s2PAFilter.setCurrentPolicy(S2PAFilter.ALL_SERVICES_DISABLE);
        s2PAFilter.disableAllSensors();
        s2PAFilter.setActive(true);
    }

    /**
     * Gets the CDDL instance.
     * @return the CDDL instance.
     */
    public static CDDL getInstance() {
        return instance;
    }

    /**
     * Starts the CDDL services.
     * The CDDL services started are: {@link }CommandService},
     * {@link QoCEvaluatorService}, {@link LocalDirectoryService}, {@link LocationService},
     * {@link AdaptationService} and {@link S2PAService}.
     *
     */
    public synchronized void startService() {

        Asserts.assertNotNull(connection, "Connection must be set in CDDL before calling startServices.");
        Asserts.assertNotNull(context, "Context must be set in CDDL before calling startServices.");

        if (!servicesStarted) {

            val cs = new Intent(context, CommandService.class);
            context.startService(cs);

            val qoc = new Intent(context, QoCEvaluatorService.class);
            context.startService(qoc);

            val ld = new Intent(context, LocalDirectoryService.class);
            context.startService(ld);

            val loc1 = new Intent(context, LocationService.class);
            context.startService(loc1);

            val ad = new Intent(context, AdaptationService.class);
            context.startService(ad);

            val s2pa = new Intent(context, S2PAService.class);
            context.startService(s2pa);

            servicesStarted = true;

        }

    }

    /**
     * Stops the CDDL services.
     * The CDDL services stopped are: {@link }CommandService},
     * {@link QoCEvaluatorService}, {@link LocalDirectoryService}, {@link LocationService},
     * {@link AdaptationService} and {@link S2PAService}.
     *
     */
    public synchronized void stopService() {

        if (servicesStarted) {

            val s2pa = new Intent(context, S2PAService.class);
            context.stopService(s2pa);

            val ad = new Intent(context, AdaptationService.class);
            context.stopService(ad);

            val loc1 = new Intent(context, LocationService.class);
            context.startService(loc1);

            val ld = new Intent(context, LocalDirectoryService.class);
            context.stopService(ld);

            val qoc = new Intent(context, QoCEvaluatorService.class);
            context.stopService(qoc);

            val cs = new Intent(context, CommandService.class);
            context.stopService(cs);

            servicesStarted = false;

        }

    }

    /**
     * Starts all communication technologies.
     * The communication technologies started are: Bluetooth Low Energy, Bluetooth Classic and
     * Internal Device Sensors.
     */
    public void startAllCommunicationTechnologies() {
        startBluetoothLowEnergyTechnology();
        startBluetoothClassicTechnology();
        startInternalSensorTechnology();
    }

    /**
     * Stops all communication technologies.
     * The communication technologies stopped are: Bluetooth Low Energy, Bluetooth Classic and
     * Internal Device Sensors.
     */
    public void stopAllCommunicationTechnologies() {
        stopBluetoothLowEnergyTechnology();
        stopBluetoothClassicTechnology();
        stopInternalSensorTechnology();
    }

    /**
     * Starts the specified communication techcnology.
     * @param technology The Id of the communication technology to be started.
     */
    public void startCommunicationTechnology(int technology) {

        if (technology == InternalTechnology.ID) {
            startInternalSensorTechnology();
        }
        else if (technology == BLETechnology.ID) {
            startBluetoothLowEnergyTechnology();
        }
        else if (technology == BTTechnology.ID) {
            startBluetoothClassicTechnology();
        }
        else {
            throw new RuntimeException("Unkown technology id: " + technology);
        }
    }

    /**
     * Stops the specified communication techcnology.
     * @param technology The Id of the communication technology to be stopped.
     */
    public void stopCommunicationTechnology(int technology) {

        if (technology == InternalTechnology.ID) {
            stopInternalSensorTechnology();
        }
        else if (technology == BLETechnology.ID) {
            stopBluetoothLowEnergyTechnology();
        }
        else if (technology == BTTechnology.ID) {
            stopBluetoothClassicTechnology();
        }
        else {
            throw new RuntimeException("Unkown technology id: " + technology);
        }

    }

    /**
     * Starts listening to all the sensors of the started technologies.
     */
    public void startAllSensors() {
        s2PAFilter.enableAllSensors();
        MHubEventBus.getDefault().postHistory(new StartAllSensorsMessage());
    }

    /**
     * Starts listening for all the sensors of the started technologies but specifying a type of delay.
     * The type of delay can be:
     *   Fastest delay = 0,
     *   Game delay = 1,
     *   UI delay = 2,
     *   Normal delay = 3
     *
     * @param delayType The type of delay.
     */
    public void startAllSensors(int delayType) {
        s2PAFilter.enableAllSensors();
        MHubEventBus.getDefault().postHistory(new StartAllSensorsMessage(delayType));
    }

    /**
     * Stops listening to all the sensors of the started technologies.
     */
    public void stopAllSensors() {
        s2PAFilter.disableAllSensors();
        MHubEventBus.getDefault().postHistory(new StopAllSensorsMessage());
    }

    /**
     * Starts listening for the specified sensor.
     *
     * @param sensorName Name of the sensor to be listened to
     */
    public void startSensor(String sensorName) {
        val startSensorMessageBySimpleName = new StartSensorMessageBySimpleName();
        startSensorMessageBySimpleName.setName(sensorName);
        startSensorMessageBySimpleName.setRate(SensorManager.SENSOR_DELAY_NORMAL);
        s2PAFilter.enableSensor(sensorName);
        MHubEventBus.getDefault().postHistory(startSensorMessageBySimpleName);
    }

    /**
     * Starts listening for the specified sensor but specifying a type of delay.
     * The type of delay can be:
     *   Fastest delay = 0,
     *   Game delay = 1,
     *   UI delay = 2,
     *   Normal delay = 3
     *
     * @param sensorName Name of the sensor to be listened to
     */
    public void startSensor(String sensorName, int delayType) {
        val startSensorMessageBySimpleName = new StartSensorMessageBySimpleName();
        startSensorMessageBySimpleName.setName(sensorName);
        startSensorMessageBySimpleName.setRate(delayType);
        s2PAFilter.enableSensor(sensorName);
        MHubEventBus.getDefault().postHistory(startSensorMessageBySimpleName);
    }

    /**
     * Starts listening for the specified sensor.
     *
     * @param id Id of the sensor to be listened to
     */
    public void startSensorById(int id) {
        val startSensorMessageById = new StartSensorMessageById();
        startSensorMessageById.setId(id);
        //s2PAFilter.enableSensor(sensorName);
        MHubEventBus.getDefault().postHistory(startSensorMessageById);
    }


    /**
     * Starts listening for the specified sensor but specifying a type of delay.
     * The type of delay can be:
     *   Fastest delay = 0,
     *   Game delay = 1,
     *   UI delay = 2,
     *   Normal delay = 3
     *
     * @param id Id of the sensor to be listened to
     */
    public void startSensorById(int id, int delayType) {
        val startSensorMessageById = new StartSensorMessageById();
        startSensorMessageById.setId(id);
        startSensorMessageById.setRate(delayType);
        //s2PAFilter.enableSensor(sensorName);
        MHubEventBus.getDefault().postHistory(startSensorMessageById);
    }

    /**
     * Stops listening to the specified sensor.
     *
     * @param id Id of the sensor to stop listening
     */
    public void stopSensorById(int id) {
        val stopSensorMessageById = new StopSensorMessageById();
        stopSensorMessageById.setId(id);
        //s2PAFilter.enableSensor(sensorName);
        MHubEventBus.getDefault().postHistory(stopSensorMessageById);
    }


    /**
     * Stops listening to the specified sensor.
     *
     * @param sensorName Name of the sensor to stop listening
     */
    public void stopSensor(String sensorName) {
        val stopSensorMessageBySimpleName = new StopSensorMessageBySimpleName();
        stopSensorMessageBySimpleName.setName(sensorName);
        s2PAFilter.disableSensor(sensorName);
        MHubEventBus.getDefault().postHistory(stopSensorMessageBySimpleName);
    }

    /**
     * Starts location sensor.
     */
    public void startLocationSensor() {
        s2PAFilter.enableSensor(LocationSensor.NAME);
        MHubEventBus.getDefault().postHistory(new StartLocationSensorMessage());
    }

    /**
     * Starts the location sensor specifying the time interval that the sensor will send the information.
     * @param interval The time interval in milliseconds that the sensor will send the information.
     */
    public void startLocationSensor(long interval) {
        s2PAFilter.enableSensor(LocationSensor.NAME);
        MHubEventBus.getDefault().postHistory(new StartLocationSensorMessage(interval));
    }

    /**
     * Stops the location sensor
     */
    public void stopLocationSensor() {
        s2PAFilter.disableSensor(LocationSensor.NAME);
        MHubEventBus.getDefault().postHistory(new StopLocationSensorMessage());
    }

    /**
     * Starts battery sensor.
     */
    public void startBatterySensor() {
        s2PAFilter.enableSensor(BatterySensor.NAME);
        MHubEventBus.getDefault().postHistory(new StartBatterySensorMessage());
    }

    /**
     * Starts the battery sensor specifying the time interval that the sensor will send the information.
     * @param interval The time interval in milliseconds that the sensor will send the information.
     */
    public void startBatterySensor(long interval) {
        s2PAFilter.enableSensor(BatterySensor.NAME);
        MHubEventBus.getDefault().postHistory(new StartBatterySensorMessage(interval));
    }

    /**
     * Stops battery sensor.
     */
    public void stopBatterySensor() {
        s2PAFilter.disableSensor(BatterySensor.NAME);
        MHubEventBus.getDefault().postHistory(new StopBatterySensorMessage());
    }

    public static String startMicroBroker(String host, String port, String webSocketPort, String passwordFile) {
        return MicroBroker.getInstance().start(host, port, webSocketPort, passwordFile);
    }

    /**
     * Starts the MQTT microbroker
     */
    public static String startMicroBroker() {
        return MicroBroker.getInstance().start();
    }

    /**
     * Stops the MQTT microbroker
     */
    public static void stopMicroBroker() {
        MicroBroker.getInstance().stop();
    }

    /**
     * Sets the connection to be used for the CDDL instance.
     * @param connection The connection to be used for the CDDL instance.
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * Gets the context used by the CDDL instance.
     * @return the context used by the CDDL instance.
     */
    public Context getContext() {
        return context;
    }

    /**
     * Sets the context to be used for the CDDL instance.
     * @param context The context to be used for the CDDL instance.
     */
    public void setContext(Context context) {
        this.context= context;
    }

    /**
     * Gets the connection used by the CDDL instance.
     * @return the connection used by the CDDL instance.
     */
    public Connection getConnection() {
        return connection;
    }


    public void setFilter(String eplFilter) {
        val cddlFilter = new CDDLFilterImpl(eplFilter);
        CDDLEventBus.getDefault().post(cddlFilter);
    }

    public void clearFilter() {
        val cddlFilter = new CDDLFilterImpl("");
        CDDLEventBus.getDefault().post(cddlFilter);
    }

    /**
     * Gets the list of all internal sensors of the device.
     * @return the list of all internal sensors of the device.
     */
    public List<Sensor> getInternalSensorList() {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensorManager.getSensorList(Sensor.TYPE_ALL);
    }

    private void startInternalSensorTechnology() {
        MHubEventBus.getDefault().postHistory(new StartCommunicationTechnologyMessage(InternalTechnology.ID));
    }

    private void stopInternalSensorTechnology() {
        MHubEventBus.getDefault().postHistory(new StopCommunicationTechnologyMessage(InternalTechnology.ID));
    }

    private void startBluetoothLowEnergyTechnology() {
        MHubEventBus.getDefault().postHistory(new StartCommunicationTechnologyMessage(BLETechnology.ID));
    }

    private void stopBluetoothLowEnergyTechnology() {
        MHubEventBus.getDefault().postHistory(new StopCommunicationTechnologyMessage(BLETechnology.ID));
    }

    private void startBluetoothClassicTechnology() {
        MHubEventBus.getDefault().postHistory(new StartCommunicationTechnologyMessage(BTTechnology.ID));
    }

    private void stopBluetoothClassicTechnology() {
        MHubEventBus.getDefault().postHistory(new StopCommunicationTechnologyMessage(BTTechnology.ID));
    }

    public void setQoS(AbstractQoS qos) {
        CDDLEventBus.getDefault().postSticky(qos);
    }

}
