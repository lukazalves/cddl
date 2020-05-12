package br.ufma.lsdi.cddl;

import br.ufma.lsdi.cddl.network.ConnectionImpl;

/**
 * MQTT connection factory.
 *
 * Created by bertodetacio on 17/05/17.
 */

public final class ConnectionFactory {

    private ConnectionFactory() {}

    /**
     * Creates a new connection to the MQTT service.
     * @return a new connection to the MQTT service
     */
    public static ConnectionImpl createConnection() {
        return new ConnectionImpl();
    }

}
