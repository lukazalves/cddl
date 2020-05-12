package br.ufma.lsdi.cddl;

import br.ufma.lsdi.cddl.listeners.IConnectionListener;
import br.ufma.lsdi.cddl.listeners.IMessageListener;
import br.ufma.lsdi.cddl.message.Message;

/**
 * MQQT connection service.
 */
public interface Connection {

    String TCP = "tcp";
    String SSL = "ssl";

    String ECLIPSE_HOST = "iot.eclipse.org";
    String DASHBOARD_HOST = "broker.mqttdashboard.com";
    String MOSCA_HOST = "test.mosca.io";
    String HIVEMQ_HOST = "broker.hivemq.com";
    String MOSQUITTO_HOST = "test.mosquitto.org";
    String LSD_HOST = "lsdi.ufma.br";
    String MICRO_BROKER_LOCAL_HOST = "localhost";

    String DEFAULT_HOST = LSD_HOST;
    String DEFAULT_PORT = "1883";
    String DEFAULT_WEBSOCKET_PORT = "8080";
    String DEFAULT_PASSWORD_FILE = "";

    void connect(String protocol, String host, String port, boolean automaticReconnect, long automaticReconnectionTime, boolean cleanSession, int connectionTimeout, int keepAliveInterval, boolean publishConnectionChangedStatus, int maxInflightMessages, String username, String password, int mqttVersion);

    /**
     * Connects to the MQTT service
     */
    void connect();

    /**
     * Reconnects to the MQTT service
     */
    void reconnect();

    /**
     * Returns whether or not the connection to the MQTT service is active.
     * True indicates that the connection is active.
     * @return whether or not the connection to the MQTT service is active.
     */
    boolean isConnected();

    /**
     * Disconnects from MQTT service
     */
    void disconnect();

    /**
     * Returns the protocol used to connect to the MQTT service.
     * @return the protocol used to connect to the MQTT service.
     */
    String getProtocol();

    /**
     * Sets the protocol used to connect to the MQTT service.
     * @param protocol the protocol used to connect to the MQTT service.
     */
    void setProtocol(String protocol);

    /**
     * Returns the host address of the MQTT service.
     * @return the host address of the MQTT service.
     */
    String getHost();

    /**
     * Sets the host address of the MQTT service.
     * @param host the host address of the MQTT service.
     */
    void setHost(String host);

    /**
     * Returns the port number used to connect to the MQTT service.
     * @return the port number used to connect to the MQTT service.
     */
    String getPort();

    /**
     * Sets the port number used to connect to the MQTT service.
     * @param port the port number used to connect to the MQTT service.
     */
    void setPort(String port);

    /**
     * Returns the time (in milliseconds) that the CDDL waits before attempting to reconnect to the MQTT service after a disconnection.
     * @return the time in milliseconds that CDDL waits before attempting to reconnect.
     */
    long getAutomaticReconnectionTime();

    /**
     * Sets the time (in milliseconds) that the CDDL has to wait before attempting to reconnect to the MQTT service when disconnection occurs.
     * @param automaticReconnectionTime the time (in milliseconds) that the CDDL has to wait before attempting to reconnect to the MQTT service.
     */
    void setAutomaticReconnectionTime(long automaticReconnectionTime);

    boolean isCleanSession();

    void setCleanSession(boolean cleanSession);

    int getConnectionTimeout();

    void setConnectionTimeout(int connectionTimeout);

    int getKeepAliveInterval();

    void setKeepAliveInterval(int keepAliveInterval);

    boolean isAutomaticReconnection();

    void setAutomaticReconnection(boolean automaticReconnection);

    boolean isPublishConnectionChangedStatus();

    void setPublishConnectionChangedStatus(boolean publishConnectionChangedStatus);

    String getPasswordFile();

    void setPasswordFile(String passwordFile);

    int getMaxInflightMessages();

    void setMaxInflightMessages(int maxInflightMessages);

    /**
     * Returns the user connected to MQTT service.
     * @return the user connected to MQTT service.
     */
    String getUsername();

    /**
     * Sets the username to connect to the MQTT service.
     * @param username the username to connect to the MQTT service.
     */
    void setUsername(String username);

    /**
     * Returns the password used to connect to MQTT service.
     * @return the password used to connect to MQTT service.
     */
    String getPassword();

    /**
     * Sets the password to use to connect to the MQTT service.
     * @param password the password to use to connect to the MQTT service.
     */
    void setPassword(String password);

    boolean isSynchronizedModeActive();

    void setSynchronizedModeActive(boolean synchronizedModeActive);

    long getRepublicationInterval();

    void setRepublicationInterval(long republicationInterval);

    boolean isPersistBufferEnable();

    void setPersistBufferEnable(boolean persistBufferEnable);

    boolean isPersistBuffer();

    void setPersistBuffer(boolean persistBuffer);

    int getPersistBufferSize();

    void setPersistBufferSize(int persistBufferSize);

    boolean isDeleteOldestMessagesFromPersistBuffer();

    void setDeleteOldestMessagesFromPersistBuffer(boolean deleteOldestMessagesFromPersistBuffer);

    /**
     * Publishes a message on MQTT service.
     * @param message message to be published.
     */
    void publish(Message message);

    /**
     * Subscribes a topic in the MQTT service.
     * @param topic Topic to be subscribed.
     * @param reliability Reliability level required.
     * @param connectionListener Listener to be used for receiving messages.
     */
    void subscribe(String topic, int reliability, IMessageListener connectionListener);

    void unsubscribe(String topic, IMessageListener connectionListener);

    /**
     * Unsubscribe from all topics.
     */
    void unsubscribeAll();

    /**
     * Registers a new connection listener.
     * @param connectionListener connection listener to be registered.
     */
    void addConnectionListener(IConnectionListener connectionListener);

    /**
     * Removes a connection listener.
     * @param connectionListener connection listener to be removed.
     */
    void removeConnectionListener(IConnectionListener connectionListener);

    /**
     * Removes all connection listeners.
     */
    void removeAllConnectionListeners();

    boolean isEnableIntermediateBuffer();

    void setEnableIntermediateBuffer(boolean enableIntermediateBuffer);

    /**
     * Sets the client id of the connection.
     * @param clientId the client id of the connection.
     */
    void setClientId(String clientId);

    /**
     * Returns the client id of the connection.
     * @return the client id of the connection.
     */
    String getClientId();

}
