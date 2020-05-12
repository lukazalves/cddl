package br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import br.pucrio.inf.lac.mhub.models.locals.LocationData;
import br.pucrio.inf.lac.mhub.models.locals.SensorDataExtended;
import br.pucrio.inf.lac.mhub.util.MHubEventBus;

/*
 * Location sensor. Uses GPS for location when available. Otherwise, uses network provider
 */
public class LocationSensor implements InternalSensor {

    /**
     * DEBUG
     */
    private static final String TAG = LocationSensor.class.getSimpleName();

    public static final int ID = -3;

    private InternalSensorListener listener;  // Listener pointing to SensorPhone class
    private Context ac;


    /**
     * Last location saved
     */
    private LocationData lastRegisteredLocation;

    /**
     * The two providers that we care
     */
    private String gpsProvider;
    private String networkProvider;

    /**
     * The location manager
     */
    private LocationManager lm;

    /**
     * Current location update interval
     */
    private Integer currentInterval;

    /**
     * GPS rate, since it consumes more battery than network
     */
    private static final int GPS_RATE = 4;

    /**
     * Time difference threshold set for two minutes
     */
    private long interval = 1000;

    public static final String NAME = "Location";

    public LocationSensor(Context context, long interval) {
        this.ac = context;
        this.interval = interval;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public synchronized void onMessageEvent(LocationData location) {

        SensorDataExtended data = new SensorDataExtended();

        data.setSensorName(NAME);

        Double[] values = {location.getLatitude(), location.getLongitude(), location.getAltitude(), Double.valueOf(location.getSpeed())};

        data.setSensorObjectValue(values);
        data.setSensorValue(values);
        data.setSensorAccuracy(Double.valueOf(location.getAccuracy()));
        data.setAvailableAttributes(values.length);
        data.setLatitude(location.getLatitude());
        data.setLongitude(location.getLongitude());
        data.setAltitude(location.getAltitude());
        data.setLocationTimestamp(location.getTime());
        data.setLocationAccuracy(Double.valueOf(location.getAccuracy()));
        data.setSpeed(Double.valueOf(location.getSpeed()));
        data.setAvailableAttributesList(new String[]{"Latitude", "Longitude", "Altitude", "Speed"});

        lastRegisteredLocation = location;

        if (listener != null) {

            listener.onInternalSensorChanged(data);
        }
        // save the location as last registered

    }

    /**
     * Decide if new location is better than older by following some basic criteria.
     *
     * @param oldLocation Old location used for comparison.
     * @param newLocation Newly acquired location compared to old one.
     * @return If new location is more accurate and suits your criteria more than the old one.
     */
    private boolean isBetterLocation(Location oldLocation, Location newLocation) {
        // If there is no old location, the new location is better
        if (oldLocation == null)
            return true;

        // Check if new location is newer in time
        long timeDelta = newLocation.getTime() - oldLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > interval;
        boolean isSignificantlyOlder = timeDelta < -interval;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer)
            return true;
            // If the new location is more than two minutes older, it must be worse
        else if (isSignificantlyOlder)
            return false;

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (newLocation.getAccuracy() - oldLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(newLocation.getProvider(), oldLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate)
            return true;
        else if (isNewer && !isLessAccurate)
            return true;
        else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider)
            return true;

        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null)
            return provider2 == null;
        return provider1.equals(provider2);
    }

    @Override
    public void start() {
        if (!MHubEventBus.getDefault().isRegistered(this)) {
            MHubEventBus.getDefault().register(this);
        }
    }

    @Override
    public void stop() {
        if (MHubEventBus.getDefault().isRegistered(this)) {
            MHubEventBus.getDefault().unregister(this);
        }
    }

    @Override
    public void setListener(InternalSensorListener listener) {
        this.listener = listener;
    }

    @Override
    public String getName() {
        return NAME;
    }


}
