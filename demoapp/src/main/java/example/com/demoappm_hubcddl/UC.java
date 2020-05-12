package example.com.demoappm_hubcddl;

import android.content.Context;
import android.widget.TextView;

import br.ufma.lsdi.cddl.CDDL;
import br.ufma.lsdi.cddl.Connection;
import br.ufma.lsdi.cddl.ConnectionFactory;
import br.ufma.lsdi.cddl.components.MOUUID;
import br.ufma.lsdi.cddl.components.TechnologyID;
import br.ufma.lsdi.cddl.listeners.IMonitorListener;
import br.ufma.lsdi.cddl.listeners.ISubscriberListener;
import br.ufma.lsdi.cddl.message.CommandMessage;
import br.ufma.lsdi.cddl.message.Message;
import br.ufma.lsdi.cddl.message.QueryResponseMessage;
import br.ufma.lsdi.cddl.message.SensorDataMessage;
import br.ufma.lsdi.cddl.message.ServiceInformationMessage;
import br.ufma.lsdi.cddl.ontology.QueryType;
import br.ufma.lsdi.cddl.pubsub.Publisher;
import br.ufma.lsdi.cddl.pubsub.PublisherFactory;
import br.ufma.lsdi.cddl.pubsub.Subscriber;
import br.ufma.lsdi.cddl.pubsub.SubscriberFactory;

public class UC {

    public static CDDL cddl = CDDL.getInstance();
    public static Connection connection;
    public static Publisher publisher;
    public static Subscriber subscriber;

    public static Connection createConnection(String clientId, String host) {
        Connection connection = ConnectionFactory.createConnection();
        connection.setClientId(clientId);
        connection.setHost(host);
        return connection;
    }

    public static CDDL initCDDL(Context context, Connection connection) {
        CDDL cddl = CDDL.getInstance();
        cddl.setConnection(connection);
        cddl.setContext(context);
        cddl.startService();
        cddl.startCommunicationTechnology(CDDL.INTERNAL_TECHNOLOGY_ID);
        return cddl;
    }

    public static void init(Context context, final TextView textView) {

        cddl.setConnection(connection);
        cddl.startService();

        //cddl.startInternalSensorTechnology();

        //cddl.startBluetoothClassicTechnology();

        //cddl.startBluetoothLowEnergyTechnology();

        //cddl.startAllSensors();

        //cddl.startSensor("Goldfish Orientation sensor");
        //cddl.startSensor("Goldfish Temperature sensor");

        //cddl.setFilter("select * from Message where availableAttributes = 3");

        //cddl.startSensor("Location(fused)");

        //cddl.startSensor("Location");

        // cddl.startSensorById(BatterySensor.ID);

        //createPublisher();

        //pubQueryInstantanea();

        //sub();

        //filtrar();

        //pubCommand();

    }

    public static void filtrarCDDL() {
        Message m;
        cddl.setFilter("select * from Message where availableAttributes = 3");
    }
    private static void createPublisher() {
        publisher = PublisherFactory.createPublisher();
        publisher.addConnection(connection);
    }

    public static void stop(Context context) {
        cddl.stopService();
        connection.disconnect();
    }

    public static void pub() {

        publisher = PublisherFactory.createPublisher();
        publisher.addConnection(connection);

        SensorDataMessage cim = new SensorDataMessage();
        cim.setServiceName("Meu serviço");
        cim.setServiceByteArray("Valor");
        publisher.publish(cim);

    }

    public static void pubCommand() {

        publisher = PublisherFactory.createPublisher();
        publisher.addConnection(connection);

        String m = "{\"characteristicUUID\": \"FFE1-0000-1000-8000-00805F9B34FB\", \"command\":[72,59,49,48,59,70]}";

        CommandMessage cm = new CommandMessage("lcmuniz@gmail.com", new MOUUID(TechnologyID.BLE.id, "D4:36:39:D8:C1:39").toString(), "HMSoft", m);
        publisher.publish(cm);

    }

    public static void pubQueryInstantanea() {
        publisher.query(QueryType.INSTANTANEOUS, "select * from ServiceInformationMessage.win:time_batch(5 sec) where publisherID = 'lcmuniz@gmail.com'");
    }

    public static void sub() {
        Connection c = ConnectionFactory.createConnection();
        c.setClientId("maria@gmail.com");
        c.setHost(Connection.LSD_HOST);
        c.setAutomaticReconnection(true);
        c.setCleanSession(false);
        c.connect();

        subscriber = SubscriberFactory.createSubscriber();
        subscriber.addConnection(c);
        subscriber.setSubscriberListener(subscriberListener);
        //subscriber.subscribeServiceByName("Goldfish Orientation sensor");
        subscriber.subscribeServiceByPublisherId("lcmuniz@gmail.com");
    }

    public static void filtrar() {
        String filtro = "select * from Message where availableAttributes = 3";
        subscriber.setFilter(filtro);
    }

    public static void limparFiltro() {
        subscriber.clearFilter();
    }

    public void limparMonitor(String queryId) {
        Subscriber subscriber = SubscriberFactory.createSubscriber();
        subscriber.addConnection(connection);
        subscriber.getMonitor().removeRule(queryId);
    }

    public String monitorar() {

        String query = "select * from SensorDataMessage where age > 3000";
        String queryId = subscriber.getMonitor().addRule(query, monitorListener);
        return queryId;

    }

    private static ISubscriberListener subscriberListener = new ISubscriberListener() {


        @Override
        public void onMessageArrived(Message message) {

            if (message instanceof SensorDataMessage) {
                final SensorDataMessage sensorDataMessage = (SensorDataMessage) message;

                System.out.println("Message received >>>"+sensorDataMessage);

            }
            if (message instanceof QueryResponseMessage) {
                QueryResponseMessage qrm = (QueryResponseMessage) message;
                for (ServiceInformationMessage sim : qrm.getServiceInformationMessageList()) {
                    System.out.println("RESPOSTA DA QUERY " + sim.getServiceName() + " - " + sim.getAccuracy() + " - " + sim.getAge());
                }
                return;
            }
        }

    };

    private IMonitorListener monitorListener = new IMonitorListener() {
        @Override
        public void onEvent(final Message message) {

            if (message instanceof SensorDataMessage) {

                System.out.println(message);
            }
        }

    };
}
