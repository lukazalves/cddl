package example.com.demoappm_hubcddl;

import android.content.Context;

import br.ufma.lsdi.cddl.CDDL;
import br.ufma.lsdi.cddl.Connection;
import br.ufma.lsdi.cddl.ConnectionFactory;

public class MainController {

    private CDDL cddl;

    public void config (Context context){
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
}
