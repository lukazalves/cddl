package example.com.demoappm_hubcddl;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Date;

import br.ufma.lsdi.cddl.CDDL;
import br.ufma.lsdi.cddl.ConnectionFactory;
import br.ufma.lsdi.cddl.listeners.IConnectionListener;
import br.ufma.lsdi.cddl.listeners.ISubscriberListener;
import br.ufma.lsdi.cddl.message.Message;
import br.ufma.lsdi.cddl.network.ConnectionImpl;
import br.ufma.lsdi.cddl.pubsub.Publisher;
import br.ufma.lsdi.cddl.pubsub.PublisherFactory;
import br.ufma.lsdi.cddl.pubsub.Subscriber;
import br.ufma.lsdi.cddl.pubsub.SubscriberFactory;
import br.ufma.lsdi.cddl.qos.TimeBasedFilterQoS;
import lombok.val;

public class MainActivity extends Activity {

    CDDL cddl;

    private TextView messageTextView;
    private View sendButton;
    private ConnectionImpl conLocal;
    private ConnectionImpl conRemota;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setPermissions();

        setViews();

        configConLocal();
        configConRemota();

        initCDDL();
        //startAccelerometer();
        //subscribeAccelerometer();

        subscribeMessage();

        //publishMessage();

        sendButton.setOnClickListener(clickListener);

    }

    private void configConRemota() {
        val host = "broker.hivemq.com";
        conRemota = ConnectionFactory.createConnection();
        conRemota.setClientId("lcmuniz@lsdi.ufma.br");
        conRemota.setHost(host);
        conRemota.addConnectionListener(connectionListener);
        conRemota.connect();
    }

    private void configConLocal() {
        val host = CDDL.startMicroBroker();
        conLocal = ConnectionFactory.createConnection();
        conLocal.setClientId("lcmuniz@gmail.com");
        conLocal.setHost(host);
        conLocal.addConnectionListener(connectionListener);
        conLocal.connect();
    }

    private void initCDDL() {
        //val host = "broker.hivemq.com";
        cddl = CDDL.getInstance();
        cddl.setConnection(conLocal);
        cddl.setContext(this);
        cddl.startService();

        cddl.startCommunicationTechnology(CDDL.INTERNAL_TECHNOLOGY_ID);
        //cddl.startLocationSensor();
        cddl.setQoS(new TimeBasedFilterQoS());
    }

    @Override
    protected void onDestroy() {
        cddl.stopLocationSensor();
        cddl.stopAllCommunicationTechnologies();
        cddl.stopService();
        conLocal.disconnect();
        CDDL.stopMicroBroker();
        super.onDestroy();
    }

    private void subscribeMessage() {
        Subscriber sub = SubscriberFactory.createSubscriber();
        sub.addConnection(conRemota);
        sub.subscribeServiceByName("Meu serviço");
        sub.subscribeServiceByName("Meu serviço remoto");

        sub.setSubscriberListener(new ISubscriberListener() {
            @Override
            public void onMessageArrived(Message message) {

                if (message.getServiceName().equals("Meu serviço")) {
                    System.out.println(message.getSourceLocationAltitude());
                    Log.d("_MAIN", "LOCAL: " + message);
                }
                if (message.getServiceName().equals("Meu serviço remoto")) {
                    Log.d("_MAIN", "REMOTO: " + message);
                }
            }
        });

    }

    private void publishMessageLocal() {

        Publisher publisher = PublisherFactory.createPublisher();
        publisher.addConnection(cddl.getConnection());

        MyMessage message = new MyMessage();
        message.setServiceName("Meu serviço");
        message.setServiceByteArray("Valor");
        publisher.publish(message);
    }

    private void publishMessageRemota() {

        Publisher publisher = PublisherFactory.createPublisher();
        publisher.addConnection(conRemota);

        MyMessage message = new MyMessage();
        message.setServiceName("Meu serviço remoto");
        message.setServiceByteArray(new Date());
        publisher.publish(message);
    }

    private void setViews() {
        sendButton = findViewById(R.id.sendButton);
        messageTextView = (TextView) findViewById(R.id.messageTexView);
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            publishMessageLocal();
            publishMessageRemota();
        }
    };

    private IConnectionListener connectionListener = new IConnectionListener() {
        @Override
        public void onConnectionEstablished() {
            messageTextView.setText("Conexão estabelecida.");
        }

        @Override
        public void onConnectionEstablishmentFailed() {
            messageTextView.setText("Falha na conexão.");
        }

        @Override
        public void onConnectionLost() {
            messageTextView.setText("Conexão perdida.");
        }

        @Override
        public void onDisconnectedNormally() {
            messageTextView.setText("Uma disconexão normal ocorreu.");
        }

    };

    private void setPermissions() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

    }

}
