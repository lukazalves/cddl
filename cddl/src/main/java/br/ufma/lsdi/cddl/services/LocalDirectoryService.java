package br.ufma.lsdi.cddl.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by lcmuniz on 05/03/17.
 */
public class LocalDirectoryService extends Service {

    private static final String TAG = LocalDirectoryService.class.getSimpleName();

    private LocalDirectoryImpl localDirectory;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        localDirectory = new LocalDirectoryImpl(this);

        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        localDirectory.close();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
