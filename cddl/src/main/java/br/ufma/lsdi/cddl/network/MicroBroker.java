package br.ufma.lsdi.cddl.network;

import org.eclipse.moquette.server.Server;

import java.io.IOException;
import java.util.Properties;

import lombok.val;

/**
 * Created by bertodetacio on 17/05/17.
 */

public final class MicroBroker {

    public static final String DEFAULT_MICRO_BROKER_HOST = "0.0.0.0";
    public static final String DEFAULT_WEBSOCKET_PORT = "8080";
    public static final String DEFAULT_PASSWORD_FILE = "";
    public static final String TCP = "tcp";
    public static final String SSL = "ssl";
    public static final String DEFAULT_PORT = "1883";

    private final String host = DEFAULT_MICRO_BROKER_HOST;
    private final String port = DEFAULT_WEBSOCKET_PORT;
    private final String passwordFile = DEFAULT_PASSWORD_FILE;
    private final String webSocketPort = DEFAULT_WEBSOCKET_PORT;

    private Server server = null;

    private static final MicroBroker instance = new MicroBroker();

    private MicroBroker() {}

    public static MicroBroker getInstance() {
        return instance;
    }

    public String start(String host, String port, String webSocketPort, String passwordFile) {
        val m_properties = new Properties();
        m_properties.put("port", port);
        m_properties.put("host", host);
        m_properties.put("websocket_port", webSocketPort);
        m_properties.put("password_file", passwordFile);

        server = new Server();
        try {
            server.startServer(m_properties);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return host;
    }

    public String  start() {
        return start(DEFAULT_MICRO_BROKER_HOST, DEFAULT_PORT, DEFAULT_WEBSOCKET_PORT, DEFAULT_PASSWORD_FILE);
    }

    public void stop() {
        if (server != null) {
            server.stopServer();
        }
    }

}
;