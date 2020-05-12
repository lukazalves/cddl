package br.pucrio.inf.lac.mhub.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;

import br.pucrio.inf.lac.mhub.components.AppConfig;
import br.pucrio.inf.lac.mhub.components.AppUtils;
import br.pucrio.inf.lac.mhub.models.base.LocalMessage;
import br.pucrio.inf.lac.mhub.models.locals.LocationData;
import br.pucrio.inf.lac.mhub.util.MHubEventBus;

/**
 * Service to obtain the location of the device.
 *
 * @author Luis Talavera
 */
public class LocationService extends Service implements LocationListener {
    /**
     * DEBUG
     */
    private static final String TAG = LocationService.class.getSimpleName();

    /**
     * The context object
     */
    private Context ac;

    /**
     * The location manager
     */
    private LocationManager lm;

    /**
     * Current location update interval
     */
    private Integer currentInterval = 1;

    /**
     * The two providers that we care
     */
    private String gpsProvider;
    private String networkProvider;

    /**
     * The Local Broadcast Manager
     */
    private LocalBroadcastManager lbm;

    /**
     * Last location saved
     */
    private Location lastLocation;

    /**
     * GPS rate, since it consumes more battery than network
     * /**
     * Time difference threshold set for two minutes
     */
    private static final int TIME_DIFFERENCE_THRESHOLD = 1000;

    /**
     * This is the object that receives interactions from clients
     */
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class for clients to access. Because we know this service always runs in
     * the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    /**
     * It gets called when the service is started.
     *
     * @param i       The intent received.
     * @param flags   Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return Using START_STICKY the service will run again if got killed by
     * the service.
     */
    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        AppUtils.logger('i', TAG, ">> Service started");
        // get the context
        ac = LocationService.this;
        // get local broadcast
        lbm = LocalBroadcastManager.getInstance(ac);
        // get location manager
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        // Configurations
        bootstrap();
        // if the service is killed by Android, service starts again
        return START_STICKY;
    }

    /**
     * When the service get destroyed by Android or manually.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        AppUtils.logger('i', TAG, ">> Service destroyed");
        // unregister broadcast
        // remove the listener
        if (lm != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            lm.removeUpdates(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * The method used to obtain the new location.
     *
     * @param newLocation The new location object.
     */
    @Override
    public void onLocationChanged(Location newLocation) {

        if (newLocation != null) {

            double speed = 0.0;

            if (lastLocation != null) {

                //long distance = calculateDistance(lastLocation.getLatitude(), location.getLatitude(), lastLocation.getLongitude(), location.getLongitude());

                double distance = lastLocation.distanceTo(newLocation);

                double diffTime = newLocation.getTime() - lastLocation.getTime();

                speed = distance / (diffTime * 1000);
            } else {
                speed = Double.valueOf(newLocation.getSpeed());
            }

            //speed = Double.valueOf(Math.round(speed));

            if (speed == Double.POSITIVE_INFINITY || speed == Double.NEGATIVE_INFINITY || speed == Double.NaN) {
                speed = Double.valueOf(Math.round(speed));
            }

            // Wait until we get a good enough location
            //if (isBetterLocation(lastLocation, location)) {

            LocationData locData = new LocationData();
            locData.setLatitude(newLocation.getLatitude());
            locData.setLongitude(newLocation.getLongitude());
            locData.setAccuracy(newLocation.getAccuracy());
            locData.setTime(newLocation.getTime());
            locData.setBearing(newLocation.getBearing());
            locData.setProvider(newLocation.getProvider());
            locData.setSpeed(speed);
            locData.setAltitude(newLocation.getAltitude());

            locData.setPriority(LocalMessage.LOW);
            locData.setRoute(ConnectionService.ROUTE_TAG);

            // post the Location object for subscribers
            //EventBus.getDefault().post(locData);

            lastLocation = newLocation;

            // envia location para qocevaluator
            MHubEventBus.getDefault().postSticky(newLocation);

            // so envia locData se existir assinantes
            if (!MHubEventBus.getDefault().hasSubscriberForEvent(LocationData.class)) return;

            MHubEventBus.getDefault().postSticky(locData);


            AppUtils.logger('i', TAG, ">> New location sent: " + locData);

            // save the location as last registered

        }

    }

    /*private static long calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        long distanceInMeters = Math.round(6371000 * c);
        return distanceInMeters;
    }*/

    @Override
    public void onProviderDisabled(String provider) {
        AppUtils.logger('i', TAG, ">> Provider disabled: " + provider);

        // If it's a provider we care about, we set it as null
        if (provider.equals(LocationManager.GPS_PROVIDER))
            gpsProvider = null;
        else if (provider.equals(LocationManager.NETWORK_PROVIDER))
            networkProvider = null;
    }

    @Override
    public void onProviderEnabled(String provider) {
        AppUtils.logger('i', TAG, ">> Provider enabled: " + provider);

        // If it's a provider we care about, we start listening
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            gpsProvider = LocationManager.GPS_PROVIDER;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            lm.requestLocationUpdates(gpsProvider,
                    currentInterval,
                    AppConfig.DEFAULT_LOCATION_MIN_DISTANCE,
                    this);
        } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            networkProvider = LocationManager.NETWORK_PROVIDER;
            lm.requestLocationUpdates(networkProvider,
                    currentInterval,
                    AppConfig.DEFAULT_LOCATION_MIN_DISTANCE,
                    this);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        String statusAsString = "Available";
        if (status == LocationProvider.OUT_OF_SERVICE)
            statusAsString = "Out of service";
        else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
            statusAsString = "Temporarily Unavailable";

        // Log any information about the status of the providers
        AppUtils.logger('i', TAG, ">> " + provider + " provider status has changed: [" + statusAsString + "]");
    }

    /**
     * The bootstrap for the location service
     */
    private void bootstrap() {
        // check for the current value
        // Start listening location updated from gps and network, if enabled
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            gpsProvider = LocationManager.GPS_PROVIDER;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            lm.requestLocationUpdates(gpsProvider,
                    currentInterval,
                    AppConfig.DEFAULT_LOCATION_MIN_DISTANCE,
                    this);

            AppUtils.logger('i', TAG, ">> GPS Location provider has been started");
        }

        // 4x faster refreshing rate since this provider doesn't consume much battery.
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            networkProvider = LocationManager.NETWORK_PROVIDER;
            lm.requestLocationUpdates(networkProvider,
                    currentInterval,
                    AppConfig.DEFAULT_LOCATION_MIN_DISTANCE,
                    this);

            AppUtils.logger('i', TAG, ">> Network Location provider has been started");
        }

        if (gpsProvider == null && networkProvider == null)
            AppUtils.logger('e', TAG, ">> No providers available");


    }


}
