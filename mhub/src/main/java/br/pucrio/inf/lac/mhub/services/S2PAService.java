package br.pucrio.inf.lac.mhub.services;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.SparseArray;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import br.pucrio.inf.lac.mhub.broadcastreceivers.BatteryReceiver;
import br.pucrio.inf.lac.mhub.broadcastreceivers.BroadcastMessage;
import br.pucrio.inf.lac.mhub.components.AppConfig;
import br.pucrio.inf.lac.mhub.components.AppUtils;
import br.pucrio.inf.lac.mhub.components.MOUUID;
import br.pucrio.inf.lac.mhub.models.base.ActuatorMessage;
import br.pucrio.inf.lac.mhub.models.base.LocalMessage;
import br.pucrio.inf.lac.mhub.models.locals.S2PASensorData;
import br.pucrio.inf.lac.mhub.models.locals.SensorData;
import br.pucrio.inf.lac.mhub.models.locals.StartCommunicationTechnologyMessage;
import br.pucrio.inf.lac.mhub.models.locals.StopCommunicationTechnologyMessage;
import br.pucrio.inf.lac.mhub.models.queries.S2PAQuery;
import br.pucrio.inf.lac.mhub.s2pa.base.Technology;
import br.pucrio.inf.lac.mhub.s2pa.base.TechnologyListenerExtended;
import br.pucrio.inf.lac.mhub.s2pa.filter.S2PAFilter;
import br.pucrio.inf.lac.mhub.s2pa.technologies.ble.BLETechnology;
import br.pucrio.inf.lac.mhub.s2pa.technologies.bt.BTTechnology;
import br.pucrio.inf.lac.mhub.s2pa.technologies.internal.InternalTechnology;
import br.pucrio.inf.lac.mhub.util.MHubEventBus;

public class S2PAService extends Service implements TechnologyListenerExtended {
    /**
     * DEBUG
     */
    private static final String TAG = S2PAService.class.getSimpleName();

    /**
     * Tag used to route the message
     */
    public static final String ROUTE_TAG = "S2PA";

    /**
     * Notifications
     */
    private NotificationManager mNManager;

    /**
     * Unique Identification Number for the Notification
     */
    //private int NOTIFICATION = R.string.service_started;

    /**
     * SparseArray of technologies
     */
    private SparseArray<Technology> technologies;

    /**
     * Time for the scans
     */
    private Integer currentTime;

    /**
     * The Local Broadcast Manager
     */
    private LocalBroadcastManager lbm;

    /**
     * Alarm Manager to check for battery status
     */
    private AlarmManager alarmMngr;

    /**
     * Alarm pending intent
     */
    private PendingIntent piAlarm;

    private BTTechnology bt;

    private BLETechnology ble;
    private boolean existBle;

    private InternalTechnology internal;

    public boolean initialized = false;


    /**
     * Handlers
     */
    private Handler mTimerHandler;
    private Handler mStopperHandler;

    public S2PAService() {
    }

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public S2PAService getService() {
            return S2PAService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        init();

        return super.onStartCommand(intent, flags, startId);
    }


    private synchronized void init() {
        AppUtils.logger('i', TAG, ">> Service started");

        // register to event bus
        // get local broadcast
        lbm = LocalBroadcastManager.getInstance(this);
        // register broadcast
        registerBroadcasts();
        // clean everything before begin
        cleanUp();
        // Restart the services
        //setAllRunningServices(true);
        // configurations
        bootstrap();
        // Start the routing from the technologies
        //startRouting();
        // if the service is killed by Android, service starts again
        initialized = true;

        if (!MHubEventBus.getDefault().isRegistered(this)) {
            MHubEventBus.getDefault().register(this);
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppUtils.logger('i', TAG, ">> Service destroyed");
        // unregister from event bus
        if (MHubEventBus.getDefault().isRegistered(this)) {
            MHubEventBus.getDefault().unregister(this);
        }
        // unregister broadcast
        unregisterBroadcasts();
        // stop the Alarm Manager
        if (piAlarm != null)
            alarmMngr.cancel(piAlarm);
        // stop receiving the information from technologies
        stopRouting();
        // Destroy technologies
        if (technologies != null) {
            for (int i = 0; i < technologies.size(); i++) {
                Technology temp = technologies.valueAt(i);
                temp.destroy();
            }
            technologies.clear();
        }
        // Stops all the services
        //setAllRunningServices(false);
        initialized = false;
        // Cancels the notification
//        if (mNManager != null)
//            mNManager.cancel(NOTIFICATION);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private synchronized void bootstrap() {

        // Initialize components
        technologies = new SparseArray<>();
        mStopperHandler = new Handler();
        mTimerHandler = new Handler();

        // create BLE  tech
        ble = new BLETechnology(this);
        existBle = ble.initialize();

        // start and AlarmManager to check for battery
        Intent iAlarm = new Intent(this, BatteryReceiver.class);
        iAlarm.setAction(BroadcastMessage.ACTION_CHECK_BATTERY_LEVEL);
        piAlarm = PendingIntent.getBroadcast(this, 0, iAlarm, PendingIntent.FLAG_CANCEL_CURRENT);
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(System.currentTimeMillis());
        alarmMngr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        alarmMngr.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                (current.getTimeInMillis() + 1000),
                AlarmManager.INTERVAL_HALF_HOUR, // Repeat
                piAlarm
        );
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public synchronized void onMessageEvent(StartCommunicationTechnologyMessage startCommunicationTechnologyMessage) {

        int technology = startCommunicationTechnologyMessage.getTechnology();

        if (technology == InternalTechnology.ID) {
            startInternalCommunicationTechnology();
        } else if (technology == BLETechnology.ID) {
            startBLECommunicationTechnology();
        } else if (technology == BTTechnology.ID) {
            startBTCommunicationTechnology();
        }
        MHubEventBus.getDefault().getHistory().remove(startCommunicationTechnologyMessage);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public synchronized void onMessageEvent(StopCommunicationTechnologyMessage stopCommunicationTechnologyMessage) {

        int technology = stopCommunicationTechnologyMessage.getTechnology();


        if (technology == InternalTechnology.ID) {
            stopInternalCommunicationTechnology();
        } else if (technology == BLETechnology.ID) {
            stopBLECommunicationTechnology();
        } else if (technology == BTTechnology.ID) {
            stopBTCommunicationTechnology();
        }
        MHubEventBus.getDefault().getHistory().remove(stopCommunicationTechnologyMessage);

    }


    public void startAllCommunicationTechnologies() {
        startBLECommunicationTechnology();
        startBTCommunicationTechnology();
        startInternalCommunicationTechnology();
    }

    public void stopAllCommunicationTechnologies() {
        stopBLECommunicationTechnology();
        stopBTCommunicationTechnology();
        stopInternalCommunicationTechnology();
    }


    private void startInternalCommunicationTechnology() {

        if (!initialized) {
            bootstrap();
        }

        if (internal == null) {

            internal = InternalTechnology.getInstance(this);
            boolean exist3 = internal.initialize();
            if (exist3) {
                technologies.append(InternalTechnology.ID, internal);
                internal.enable();
                internal.setListener(this);
            }
        }

    }

    private void stopInternalCommunicationTechnology() {

        if (!initialized) {
            bootstrap();
        }

        if (internal != null) {
            technologies.remove(BLETechnology.ID);
            internal.destroy();
            internal = null;
        }
    }


    private void startBTCommunicationTechnology() {

        if (!initialized) {
            bootstrap();
        }

        if (bt == null) {

            bt = BTTechnology.getInstance(this);
            boolean exist2 = bt.initialize();
            if (exist2) {
                technologies.append(BTTechnology.ID, bt);
                bt.setListener(this);
                bt.enable();
                bt.getBtDevicesScanner().setRepeatScan(true);
                bt.startScan(true);
                // bt.connect("C8:3E:99:0D:DA:BD");
            }
        }
    }

    private void stopBTCommunicationTechnology() {
        if (bt != null) {
            technologies.remove(BTTechnology.ID);
            bt.destroy();
            bt = null;
        }

    }


    private void startBLECommunicationTechnology() {

        if (existBle) {
            technologies.append(BLETechnology.ID, ble);
            ble.setListener(this);
            ble.enable();
            ble.startScan(true);
        }

    }

    private void stopBLECommunicationTechnology() {
        if (existBle) {
            technologies.remove(BLETechnology.ID);
            ble.stopScan();
        }
    }


    /**
     * It cleans up everything it needs.
     */
    private void cleanUp() {
        Boolean saved = AppUtils.saveIsConnected(this, false);
        if (!saved)
            AppUtils.logger('e', TAG, ">> isConnected flag not saved");
    }

    /**
     * Stops or Restarts all services
     */
    private void setAllRunningServices(boolean start) {

        Intent iConn = new Intent(this, ConnectionService.class);
        Intent iLoc = new Intent(this, LocationService.class);
        Intent iAdap = new Intent(this, AdaptationService.class);
        Intent iMepa = new Intent(this, MEPAService.class);

        // stop all services
        if (AppUtils.isMyServiceRunning(this, ConnectionService.class.getName()))
            //  stopService(iConn);

            if (AppUtils.isMyServiceRunning(this, AdaptationService.class.getName()))
                //  stopService(iAdap);

                if (AppUtils.isMyServiceRunning(this, LocationService.class.getName()))
                    stopService(iLoc);

        if (AppUtils.isMyServiceRunning(this, MEPAService.class.getName()))
            // stopService(iMepa);

            // start services
            if (start) {
                // startService(iConn);

                // lcmunix: comentei a linha abaixo para o servico de localizao iniciar.
                //if (AppUtils.getCurrentLocationService(context))
                //startService(iLoc);

                //startService(iAdap);

                if (AppUtils.getCurrentMEPAService(this)) {
                    // startService(iMepa);
                }

            }
    }

    /**
     * Starts the routing by schedule a scan on some periods
     *
     * @return if there are technologies to scan
     */
    public boolean startRouting() {
        if (technologies.size() > 0) {
            // check for the current value
            currentTime = AppUtils.getCurrentScanInterval(this);
            if (currentTime == null) // if null get the default
                currentTime = AppConfig.DEFAULT_SCAN_INTERVAL_HIGH;
            // save the current scan interval to SPREF
            AppUtils.saveCurrentScanInterval(this, currentTime);
            // Start the timer
            mTimerHandler.postDelayed(mDoScan, AppConfig.DEFAULT_DELAY_SCAN_PERIOD);

            return true;
        }
        return false;
    }

    private void stopRouting() {
        if (mTimerHandler != null)
            mTimerHandler.removeCallbacks(mDoScan);
    }

    // Runnable that takes care of start the scans
    private Runnable mDoScan = new Runnable() {
        @Override
        public void run() {
            Boolean autoconnect = AppUtils.getCurrentAutoconnectMO(S2PAService.this);

            for (int i = 0; i < technologies.size(); i++) {
                Technology temp = technologies.valueAt(i);
                temp.startScan(autoconnect);
            }
            // Stops the scan after the default time
            mStopperHandler.postDelayed(mStopScan, AppConfig.DEFAULT_SCAN_PERIOD);
        }
    };

    // Runnable that takes care of stopping the scans
    private Runnable mStopScan = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < technologies.size(); i++) {
                Technology temp = technologies.valueAt(i);
                temp.stopScan();
            }
            // Starts a new scan
            mTimerHandler.postDelayed(mDoScan, currentTime);
        }
    };

    @Override
    public void onMObjectFound(MOUUID mobileObject, Double rssi) {
        AppUtils.logger('i', TAG, ">> MObject Found: " + mobileObject.toString());

        SensorData sensorData = new SensorData();
        sensorData.setMouuid(mobileObject.toString());
        sensorData.setSignal(rssi);
        sensorData.setAction(SensorData.FOUND);

        sensorData.setPriority(LocalMessage.HIGH);
        sensorData.setRoute(ConnectionService.ROUTE_TAG);

        MHubEventBus.getDefault().post(sensorData);
    }

    @Override
    public void onMObjectConnected(MOUUID mobileObject) {
        AppUtils.logger('i', TAG, ">> MObject Connected: " + mobileObject.toString());

        SensorData sensorData = new SensorData();
        sensorData.setMouuid(mobileObject.toString());
        sensorData.setAction(SensorData.CONNECTED);

        sensorData.setPriority(LocalMessage.HIGH);
        sensorData.setRoute(ConnectionService.ROUTE_TAG);

        MHubEventBus.getDefault().post(sensorData);
    }

    @Override
    public void onMObjectDisconnected(MOUUID mobileObject, List<String> services) {
        AppUtils.logger('i', TAG, ">> MObject Disconnected: " + mobileObject.toString());

        SensorData sensorData = new SensorData();
        sensorData.setMouuid(mobileObject.toString());
        sensorData.setAction(SensorData.DISCONNECTED);

        sensorData.setPriority(LocalMessage.HIGH);
        sensorData.setRoute(ConnectionService.ROUTE_TAG);

        MHubEventBus.getDefault().postSticky(sensorData);
    }

    @Override
    public void onMObjectServicesDiscovered(MOUUID mobileObject, List<String> services) {
        AppUtils.logger('i', TAG, ">> MObject Services Discovered: " + mobileObject.toString());

        SensorData sensorData = new SensorData();
        sensorData.setMouuid(mobileObject.toString());
        sensorData.setAction(SensorData.DISCOVERED);

        sensorData.setPriority(LocalMessage.HIGH);
        sensorData.setRoute(ConnectionService.ROUTE_TAG);

        sensorData.setServiceList(services);

        MHubEventBus.getDefault().postSticky(sensorData);

    }

    @Override
    public void onMObjectValueRead(MOUUID mobileObject, Double rssi, String serviceName, Double[] values) {
        AppUtils.logger( 'i', TAG, ">> MObject Read: " + mobileObject.toString() + " Service: " + serviceName + " - Value: " + Arrays.toString( values ) );

        SensorData sensorData = new SensorData();
        sensorData.setMouuid(mobileObject.toString());
        sensorData.setSignal(rssi);
        sensorData.setSensorName(serviceName);
        sensorData.setSensorValue(values);
        sensorData.setAction(SensorData.READ);
        sensorData.setPriority(LocalMessage.LOW);
        sensorData.setRoute(ConnectionService.ROUTE_TAG + "|" + MEPAService.ROUTE_TAG);

        S2PAFilter s2PAFilter = S2PAFilter.getInstance();
        if (s2PAFilter.canPass(mobileObject, sensorData.getSensorName(), sensorData.getSensorValue())) {
            MHubEventBus.getDefault().post(sensorData);
        }

    }

    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public synchronized void onMessageEvent(S2PASensorData s2PASensorData) {
        onMObjectValueRead(s2PASensorData.getMouuid(), s2PASensorData.getSensorData());
    }

    @Override
    public void onMObjectValueRead(MOUUID mobileObject, SensorData sensorData) {
        ////AppUtils.logger( 'i', TAG, ">> MObject: " + mobileObject.toString() + " Service: " + serviceName + " - Value: " + Arrays.toString( values ) );
        sensorData.setAction(SensorData.READ);
        sensorData.setPriority(LocalMessage.LOW);
        sensorData.setRoute(ConnectionService.ROUTE_TAG + "|" + MEPAService.ROUTE_TAG);

        S2PAFilter s2PAFilter = S2PAFilter.getInstance();
        if (s2PAFilter.canPass(mobileObject, sensorData.getSensorName(), sensorData.getSensorValue())) {
            MHubEventBus.getDefault().post(sensorData);
        }

    }

    /**
     * Register/Unregister the Broadcast Receivers.
     */
    private void registerBroadcasts() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BroadcastMessage.ACTION_CHANGE_SCAN_INTERVAL);
        lbm.registerReceiver(mS2PABroadcastReceiver, filter);
    }

    private void unregisterBroadcasts() {
        if (lbm != null)
            lbm.unregisterReceiver(mS2PABroadcastReceiver);
    }

    @SuppressWarnings("unused") // it's actually used to receive s2pa query events
    @Subscribe
    public void onMessageEvent(S2PAQuery query) {
        switch (query.getType()) {
            case ADD:
                String target = query.getTarget();
                List<String> devices = query.getDevices();

                try {
                    if (target.equals("black")) {
                        for (String temp : devices) {
                            MOUUID device = MOUUID.fromString(temp);
                            Technology technology = technologies.get(device.getTechnologyID());
                            technology.addToBlackList(device.getAddress());
                        }
                    } else if (target.equals("white")) {
                        for (String temp : devices) {
                            MOUUID device = MOUUID.fromString(temp);
                            Technology technology = technologies.get(device.getTechnologyID());
                            technology.addToWhiteList(device.getAddress());
                        }
                    }
                } catch (StringIndexOutOfBoundsException ex) {
                    AppUtils.sendErrorMessage(ROUTE_TAG, ex.getMessage());
                }
                break;

            case REMOVE:
                break;
        }
    }

    /**
     * The broadcast receiver.
     */
    public BroadcastReceiver mS2PABroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            String action = i.getAction();
            /* Broadcast: ACTION_CHANGE_SCAN_INTERVAL */
            /* ****************************************** */
            if (action.equals(BroadcastMessage.ACTION_CHANGE_SCAN_INTERVAL)) {
                if (!AppUtils.getCurrentEnergyManager(S2PAService.this))
                    return;

                // obtain the new value from the EXTRA
                currentTime = i.getIntExtra(BroadcastMessage.EXTRA_CHANGE_SCAN_INTERVAL, -1);
                // problem getting the value, set the default value
                if (currentTime < 0)
                    currentTime = AppConfig.DEFAULT_SCAN_INTERVAL_HIGH;
                // save the preferences with the new value
                AppUtils.saveCurrentScanInterval(S2PAService.this, currentTime);
                AppUtils.logger('d', TAG, ">> Current time" + currentTime);
            }
        }
    };

    @Subscribe
    public void onMessageEvent(ActuatorMessage actuatorMessage) {
        int tecId = actuatorMessage.getMouuid().getTechnologyID();
        Technology technology = technologies.get(tecId);
        if (technology != null) {
            technology.writeSensorValue(actuatorMessage.getMouuid().getAddress(),
                    actuatorMessage.getServiceName(), actuatorMessage.getServiceValue());
        }

    }

}