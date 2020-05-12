package br.ufma.lsdi.cddl.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by lcmuniz on 05/03/17.
 */
public class CommandService extends Service {

    private static final String TAG = CommandService.class.getSimpleName();

    private CommandServiceImpl command;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        command = new CommandServiceImpl();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        command.close();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
