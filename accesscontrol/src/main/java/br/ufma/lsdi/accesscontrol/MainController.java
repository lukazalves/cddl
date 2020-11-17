package br.ufma.lsdi.accesscontrol;

import android.content.Context;
import android.hardware.Sensor;

import java.util.List;

import br.ufma.lsdi.cddl.CDDL;
import br.ufma.lsdi.cddl.listeners.ISubscriberListener;
import br.ufma.lsdi.cddl.pubsub.Subscriber;
import br.ufma.lsdi.cddl.pubsub.SubscriberFactory;

public class MainController {

    private CDDL cddl;
    private String currentSensor;
    private Subscriber subscriber;

    public void config(Context context){

    }

    public List<Sensor> getInternalListSensor() {

        return cddl.getInternalSensorList();
    }

    public void startSensor(String selectedSendor) {
        cddl.startSensor(selectedSendor);
        currentSensor = selectedSendor;
    }

    public void subscribeServiceByName(String selectedSensor){
        subscriber.subscribeServiceByName(selectedSensor);
    }

    public void configSubscriber(){
        subscriber = SubscriberFactory.createSubscriber();
        subscriber.addConnection(cddl.getConnection());
    }

    public void setListener(ISubscriberListener listener) {
        subscriber.setSubscriberListener(listener);
    }

    public void stopCurrentSensor(){
        cddl.stopSensor(currentSensor);
    }
}
