package br.ufma.lsdi.cddl.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by lcmuniz on 05/03/17.
 */
public class QoCEvaluatorService extends Service {

    private static final String TAG = QoCEvaluatorService.class.getSimpleName();

    private QoCEvaluatorImpl qocEvaluator;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        qocEvaluator = new QoCEvaluatorImpl(this);

        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        qocEvaluator.close();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
