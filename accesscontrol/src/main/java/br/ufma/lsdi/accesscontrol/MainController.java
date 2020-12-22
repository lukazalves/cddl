/*package br.ufma.lsdi.accesscontrol;

public class MainController {
}
*/

package br.ufma.lsdi.accesscontrol;

import android.content.Context;
import android.hardware.Sensor;

import java.util.List;

import br.ufma.lsdi.cddl.CDDL;
import br.ufma.lsdi.cddl.Connection;
import br.ufma.lsdi.cddl.ConnectionFactory;
import br.ufma.lsdi.cddl.listeners.ISubscriberListener;
import br.ufma.lsdi.cddl.pubsub.Subscriber;
import br.ufma.lsdi.cddl.pubsub.SubscriberFactory;

public class MainController {

    private CDDL cddl;
    private String currentSensor;
    private Subscriber subscriber;

    public void config(Context context){

        String host = CDDL.startMicroBroker();

        Connection connection = ConnectionFactory.createConnection();
        connection.setHost(host);
        connection.setClientId("lucasalves@lsdi.ufma.br");
        connection.connect();

        cddl = CDDL.getInstance();
        cddl.setConnection(connection);
        cddl.setContext(context);

        cddl.startService();

        cddl.startCommunicationTechnology(CDDL.INTERNAL_TECHNOLOGY_ID);
    }

    public List<Sensor> getInternalSensorList() {

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
