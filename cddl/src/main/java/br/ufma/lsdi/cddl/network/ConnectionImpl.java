package br.ufma.lsdi.cddl.network;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

import br.pucrio.inf.lac.mhub.components.AppUtils;
import br.ufma.lsdi.cddl.Connection;
import br.ufma.lsdi.cddl.listeners.IConnectionListener;
import br.ufma.lsdi.cddl.listeners.IMessageListener;
import br.ufma.lsdi.cddl.message.ConnectionChangedStatusMessage;
import br.ufma.lsdi.cddl.message.EventQueryMessage;
import br.ufma.lsdi.cddl.message.EventQueryResponseMessage;
import br.ufma.lsdi.cddl.message.LivelinessMessage;
import br.ufma.lsdi.cddl.message.Message;
import br.ufma.lsdi.cddl.message.MessageGroup;
import br.ufma.lsdi.cddl.message.ObjectConnectedMessage;
import br.ufma.lsdi.cddl.message.ObjectDisconnectedMessage;
import br.ufma.lsdi.cddl.message.ObjectDiscoveredMessage;
import br.ufma.lsdi.cddl.message.ObjectFoundMessage;
import br.ufma.lsdi.cddl.message.QueryMessage;
import br.ufma.lsdi.cddl.message.QueryResponseMessage;
import br.ufma.lsdi.cddl.message.SensorDataMessage;
import br.ufma.lsdi.cddl.message.ServiceInformationMessage;
import br.ufma.lsdi.cddl.util.Asserts;
import br.ufma.lsdi.cddl.util.Time;
import br.ufma.lsdi.cddl.util.Topic;
import lombok.val;

public final class ConnectionImpl implements IMqttActionListener, MqttCallback, Connection {

    private static final String TAG = ConnectionImpl.class.getSimpleName();

    private String protocol = TCP;
    private String port = DEFAULT_PORT;
    private String webSocketPort = DEFAULT_WEBSOCKET_PORT;
    private String passwordFile = DEFAULT_PASSWORD_FILE;

    private long automaticReconnectionTime = 1000;
    private boolean cleanSession = MqttConnectOptions.CLEAN_SESSION_DEFAULT;
    private int connectionTimeout = MqttConnectOptions.CONNECTION_TIMEOUT_DEFAULT;
    private int keepAliveInterval = MqttConnectOptions.KEEP_ALIVE_INTERVAL_DEFAULT;
    private final MemoryPersistence memoryPersistence = new MemoryPersistence();
    private MqttAsyncClient mqttClient;
    private final HashMap<IMessageListener, List<String>> listeners = new HashMap<>();
    private String lastUri = null;
    private boolean automaticReconnection = true;
    private boolean requestDisconnect = false;
    private boolean publishConnectionChangedStatus = false;
    private final ArrayList<Message> deliveryFailedMessages = new ArrayList<>();
    private RepublishMessagesTimerTask republishMessagesTimerTask = null;

    private final Timer timer = new Timer();

    private int maxInflightMessages = MqttConnectOptions.MAX_INFLIGHT_DEFAULT;
    private String username = "username";
    private String password = "password";
    private MqttConnectOptions options = null;
    private boolean synchronizedModeActive = false;
    private long republicationInterval = 1000;
    private boolean persistBufferEnable = DisconnectedBufferOptions.DISCONNECTED_BUFFER_ENABLED_DEFAULT;
    private boolean persistBuffer = DisconnectedBufferOptions.PERSIST_DISCONNECTED_BUFFER_DEFAULT;
    private int persistBufferSize = DisconnectedBufferOptions.DISCONNECTED_BUFFER_SIZE_DEFAULT;
    private boolean deleteOldestMessagesFromPersistBuffer = DisconnectedBufferOptions.PERSIST_DISCONNECTED_BUFFER_DEFAULT;
    private int mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1;
    private final String connectionId = UUID.randomUUID().toString();
    private final Set<Subscription> subscriptionPendencies = new ConcurrentSkipListSet<Subscription>();
    private final ArrayList<IConnectionListener> connectionListeners = new ArrayList<IConnectionListener>();
    private boolean enableIntermediateBuffer = false;
    private final CheckDeliveryTimerTask checkDeliveryTimerTask = null;
    private final long retryInterval = 20000;
    private int messageDeliverySuccess = 0;
    private int messageReceived = 0;
    private int messageDuplicatedReceived = 0;

    private String clientId;
    private String host;

    public ConnectionImpl() {
        Collections.synchronizedList(deliveryFailedMessages);
        Collections.synchronizedList(connectionListeners);
    }

    @Override
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    @Override
    public void connect(String protocol, String host, String port, boolean automaticReconnection, long automaticReconnectionTime, boolean cleanSession, int connectionTimeout, int keepAliveInterval, boolean publishConnectionChangedStatus, int maxInflightMessages, String username, String password, int mqttVersion) {

        Asserts.assertNotNull(clientId, "Connection: Client Id must be set before calling connect().");

        Asserts.assertNotNull(port, "Port can not be null.");
        Asserts.assertNotNull(host, "Host can not be null.");
        Asserts.assertNotNull(protocol, "Protocol can not be null.");
        Asserts.assertNotNull(password, "Password can not be null.");

        if (!isConnected()) {
            val uri =  protocol + "://" + host + ":" + port;
            try {
                lastUri =  uri;
                this.automaticReconnection = automaticReconnection;
                this.automaticReconnectionTime = automaticReconnectionTime;
                options = new MqttConnectOptions();
                this.cleanSession = cleanSession;
                options.setCleanSession(cleanSession);
                this.keepAliveInterval = keepAliveInterval;
                options.setKeepAliveInterval(keepAliveInterval);
                this.connectionTimeout = connectionTimeout;
                options.setConnectionTimeout(connectionTimeout);
                this.publishConnectionChangedStatus = publishConnectionChangedStatus;
                options.setMaxInflight(maxInflightMessages);
                this.maxInflightMessages = maxInflightMessages;
                options.setUserName(username);
                this.username = username;
                options.setPassword(password.toCharArray());
                this.password = password;
                options.setMqttVersion(mqttVersion);
                this.mqttVersion = mqttVersion;
                if (isPublishConnectionChangedStatus()) {
                    val connectionChangedStatusMessage = new ConnectionChangedStatusMessage();
                    connectionChangedStatusMessage.setStatus(ConnectionChangedStatusMessage.CLIENT_DESCONNECTED_BY_FAILURE);
                    val topic = Topic.connectionChangedStatusTopic(clientId);
                    options.setWill(topic, connectionChangedStatusMessage.toString().getBytes(), 2, false);
                }
                val disconnectedBufferOptions = new DisconnectedBufferOptions();
                disconnectedBufferOptions.setBufferEnabled(persistBufferEnable);
                disconnectedBufferOptions.setPersistBuffer(persistBuffer);
                disconnectedBufferOptions.setBufferSize(persistBufferSize);
                mqttClient = new MqttAsyncClient(uri, clientId + connectionId, memoryPersistence);
                mqttClient.setBufferOpts(disconnectedBufferOptions);
                mqttClient.setCallback(this);
                val start = Time.getCurrentTimestamp();
                val token = mqttClient.connect(options, null, this);
                token.waitForCompletion();
                val finish = Time.getCurrentTimestamp();
                AppUtils.logger('i', TAG, ">>> Tempo de Conexão com Broker MQTT " + (finish - start) + " ms");
            } catch (MqttException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void connect() {
        connect(protocol, host, port, automaticReconnection, automaticReconnectionTime, cleanSession, connectionTimeout, keepAliveInterval, publishConnectionChangedStatus, maxInflightMessages, username, password, mqttVersion);
    }

    @Override
    public synchronized void disconnect() {
        requestDisconnect = true;
        if (isConnected()) {
            try {
                if (isPublishConnectionChangedStatus()) {
                    val connectionChangedStatusMessage = new ConnectionChangedStatusMessage();
                    connectionChangedStatusMessage.setStatus(ConnectionChangedStatusMessage.CLIENT_SELF_DESCONNECTED);
                    publishConnectionStatusMessage(connectionChangedStatusMessage);
                }
                val token = mqttClient.disconnect();

                token.waitForCompletion();
                if (!isConnected()) {
                    AppUtils.logger('i', TAG, ">>> Conexão com o Broker MQTT terminada.");
                    notifyDisconnection();
                }
            } catch (MqttException e) {
                e.printStackTrace();

            }

        }

    }

    @Override
    public void onSuccess(IMqttToken iMqttToken) {
        if (republishMessagesTimerTask == null) {
            republishMessagesTimerTask = new RepublishMessagesTimerTask();
            timer.schedule(republishMessagesTimerTask, republicationInterval);
        }

        AppUtils.logger('i', TAG, ">>> Conectado com o Broker MQTT.");
        checkSubscriptionPendencies();
        notifyConnectionSuccess();
        requestDisconnect = false;
        if (isPublishConnectionChangedStatus()) {
            val connectionChangedStatusMessage = new ConnectionChangedStatusMessage();
            connectionChangedStatusMessage.setStatus(ConnectionChangedStatusMessage.CLIENT_CONNECTED);
            publishConnectionStatusMessage(connectionChangedStatusMessage);
        }

    }

    @Override
    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
        AppUtils.logger('e', TAG, ">>> Conexão com o Broker MQQT falhou.");
        notifyConnectionFalied();
        if (!requestDisconnect && automaticReconnection) {
            reconnect();
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        AppUtils.logger('e', TAG, ">>> Conexão com o Broker MQTT perdida. Razão: " + throwable.toString());
        notifyConnectionLost();
        if (!requestDisconnect && automaticReconnection) {
            reconnect();
        }
    }

    @Override
    public synchronized void publish(Message message) {

        if (!mqttClient.isConnected()) return;

        Asserts.assertNotNull(message, "Message can not be null");

        if (isTopicInvalid(message.getTopic())) return;

        val topic = message.getTopic().replace("CLIENT_ID", clientId);
        message.setTopic(topic);

        message.setPublisherID(clientId);
        message.setDelivered(false);
        message.setPublicationTimestamp(Time.getCurrentTimestamp());
        message.setPayload(message.toString().getBytes());
        AppUtils.logger('i', TAG, ">>> Publicando mensagem: " + message);
        try {
            val deliveredMessageActionLister = new DeliveredMessageActionLister(message);
            mqttClient.publish(message.getTopic(), message, null, deliveredMessageActionLister);
        } catch (Exception e) {
            AppUtils.logger('e', TAG, ">>> Publicação da mensagem falhou. Mensagem:" + message);
            message.setDeliveredFailed(true);
            if (!cleanSession && message.getQos() > 0 && !deliveryFailedMessages.contains(message) && isEnableIntermediateBuffer()) {
                deliveryFailedMessages.add(message);
            }
        }
        while (synchronizedModeActive && !message.isDelivered()) {
            //  AppUtils.logger('i', TAG, ">> Waiting delivery...");
            lock();
            //  AppUtils.logger('i', TAG, ">> Delivery completed.");
        }

    }

    @Override
    public synchronized void subscribe(String topic, int reliability, IMessageListener listener) {

        if (isTopicInvalid(topic)) return;

        topic = topic.replace("CLIENT_ID", clientId);

        if (isConnected()) {
            try {
                mqttClient.subscribe(topic, reliability);
                registerListenerAndTopic(listener, topic);
                AppUtils.logger('i', TAG, ">>> Tópico assinado: " + topic);
                val collection = new ArrayList<Subscription>();
                for (Subscription subscription : subscriptionPendencies) {
                    if (subscription.getTopic().equalsIgnoreCase(topic) && subscription.getAction().equalsIgnoreCase("subscribe")) {
                        collection.add(subscription);
                    }
                }
                subscriptionPendencies.removeAll(collection);
            } catch (MqttException e) {
                subscriptionPendencies.add(new Subscription(topic, reliability, "subscribe", listener));
                e.printStackTrace();
            }
        } else {
            subscriptionPendencies.add(new Subscription(topic, reliability, "subscribe", listener));
        }
    }

    @Override
    public synchronized void unsubscribe(String topic, IMessageListener listener) {

        if (isTopicInvalid(topic)) return;

        topic = topic.replace("CLIENT_ID", clientId);

        if (isConnected()) {
            try {
                mqttClient.unsubscribe(topic);
                unregisterListenerAndTopic(listener, topic);
                AppUtils.logger('i', TAG, ">>> Cancelada assinatura do tópico: " + topic);
                val collection = new ArrayList<Subscription>();
                for (Subscription subscription : subscriptionPendencies) {
                    if (subscription.getTopic().equalsIgnoreCase(topic) && subscription.getAction().equalsIgnoreCase("unsubscribe")) {
                        collection.add(subscription);
                    }
                }
                subscriptionPendencies.removeAll(collection);
            } catch (MqttException e) {
                subscriptionPendencies.add(new Subscription(topic, 0, "unsubscribe", listener));
                e.printStackTrace();
            }
        } else {
            subscriptionPendencies.add(new Subscription(topic, 0, "unsubscribe", listener));
        }
    }

    @Override
    public synchronized void unsubscribeAll() {

        for (IMessageListener listener : listeners.keySet()) {
            val topics = listeners.get(listener);
            for (String topic : topics) {
                try {
                    mqttClient.unsubscribe(topic);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {

        messageReceived++;

        if (mqttMessage.isDuplicate()) {
            messageDuplicatedReceived++;
        }

        //AppUtils.logger('i', TAG, messageReceived + " mensagens recebidas");
        //AppUtils.logger('i', TAG, messageDuplicatedReceived + " mensagens repetidas recebidas ");
        //AppUtils.logger('i', TAG, messageReceived - messageDuplicatedReceived + " mensagens não repetidas recebidas");

        if (isTopicInvalid(topic)) return;

        byte[] payload = mqttMessage.getPayload();

        if (payload != null) {


            val message = Message.convertFromPayload(payload);

            if (message == null) {
                for (IMessageListener listener : listeners.keySet()) {

                    val subscriptions = listeners.get(listener);

                    val message2 = new Message();
                    message2.setPayload(mqttMessage.getPayload());

                    if (subscriptions.contains(topic)) {
                        listener.onMessageArrived(message2);
                    }

                }
            } else if (message.getClassName().equalsIgnoreCase(MessageGroup.class.getName())) {

                val messageGroup = (MessageGroup) Message.convertFromPayload(payload, MessageGroup.class);

                for (Message msg : messageGroup.takeAll()) {
                    payload = msg.toString().getBytes();
                    forward(topic, msg, payload);
                }


            } else {
                forward(topic, message, payload);
            }
        }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public void setProtocol(String protocol) {
        Asserts.assertNotNull(protocol, "Protocol can not be null");
        this.protocol = protocol;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public void setHost(String host) {
        Asserts.assertNotNull(host, "Host can not be null");
        this.host = host;
    }

    @Override
    public String getPort() {
        return port;
    }

    @Override
    public void setPort(String port) {
        Asserts.assertNotNull(port, "Port can not be null");
        this.port = port;
    }

    @Override
    public long getAutomaticReconnectionTime() {
        return automaticReconnectionTime;
    }

    @Override
    public void setAutomaticReconnectionTime(long automaticReconnectionTime) {
        this.automaticReconnectionTime = automaticReconnectionTime;
    }

    @Override
    public boolean isCleanSession() {
        return cleanSession;
    }

    @Override
    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
        if (cleanSession) {
            deliveryFailedMessages.clear();
        }
    }

    @Override
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    @Override
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    @Override
    public void setKeepAliveInterval(int keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    @Override
    public boolean isAutomaticReconnection() {
        return automaticReconnection;
    }

    @Override
    public void setAutomaticReconnection(boolean automaticReconnection) {
        this.automaticReconnection = automaticReconnection;
    }

    @Override
    public boolean isPublishConnectionChangedStatus() {
        return publishConnectionChangedStatus;
    }

    @Override
    public void setPublishConnectionChangedStatus(boolean publishConnectionChangedStatus) {
        this.publishConnectionChangedStatus = publishConnectionChangedStatus;
    }

    @Override
    public String getPasswordFile() {
        return passwordFile;
    }

    @Override
    public void setPasswordFile(String passwordFile) {
        this.passwordFile = passwordFile;
    }

    @Override
    public int getMaxInflightMessages() {
        return maxInflightMessages;
    }

    @Override
    public void setMaxInflightMessages(int maxInflightMessages) {
        this.maxInflightMessages = maxInflightMessages;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        Asserts.assertNotNull(password, "Password can not be null");
        this.password = password;
    }

    @Override
    public boolean isSynchronizedModeActive() {
        return synchronizedModeActive;
    }

    @Override
    public void setSynchronizedModeActive(boolean synchronizedModeActive) {
        this.synchronizedModeActive = synchronizedModeActive;
    }

    @Override
    public long getRepublicationInterval() {
        return republicationInterval;
    }

    @Override
    public void setRepublicationInterval(long republicationInterval) {
        this.republicationInterval = republicationInterval;
    }

    @Override
    public boolean isPersistBufferEnable() {
        return persistBufferEnable;
    }

    @Override
    public void setPersistBufferEnable(boolean persistBufferEnable) {
        this.persistBufferEnable = persistBufferEnable;
    }

    @Override
    public boolean isPersistBuffer() {
        return persistBuffer;
    }

    @Override
    public void setPersistBuffer(boolean persistBuffer) {
        this.persistBuffer = persistBuffer;
    }

    @Override
    public int getPersistBufferSize() {
        return persistBufferSize;
    }

    @Override
    public void setPersistBufferSize(int persistBufferSize) {
        this.persistBufferSize = persistBufferSize;
    }

    @Override
    public boolean isDeleteOldestMessagesFromPersistBuffer() {
        return deleteOldestMessagesFromPersistBuffer;
    }

    @Override
    public boolean isEnableIntermediateBuffer() {
        return enableIntermediateBuffer;
    }

    @Override
    public synchronized void setEnableIntermediateBuffer(boolean enableIntermediateBuffer) {
        this.enableIntermediateBuffer = enableIntermediateBuffer;
        deliveryFailedMessages.clear();
    }

    @Override
    public void setDeleteOldestMessagesFromPersistBuffer(boolean deleteOldestMessagesFromPersistBuffer) {
        this.deleteOldestMessagesFromPersistBuffer = deleteOldestMessagesFromPersistBuffer;
    }

    @Override
    public synchronized void addConnectionListener(IConnectionListener connectionListener) {
        Asserts.assertNotNull(connectionListener, "Connection listener can not be null");
        if (!connectionListeners.contains(connectionListener)) {
            connectionListeners.add(connectionListener);
        }
    }

    @Override
    public synchronized void removeConnectionListener(IConnectionListener connectionListener) {
        Asserts.assertNotNull(connectionListener, "Connection listener can not be null");
        if (connectionListeners.contains(connectionListener)) {
            connectionListeners.remove(connectionListener);
        }
    }

    @Override
    public synchronized void removeAllConnectionListeners() {
        connectionListeners.clear();
    }

    private void forward(String topic, Message message, byte[] payload) {

        if (isTopicInvalid(topic)) return;

        if (message.getClassName().equalsIgnoreCase(SensorDataMessage.class.getName())) {
            val sensorDataMessage = (SensorDataMessage) Message.convertFromPayload(payload, SensorDataMessage.class);

            val publisherId = sensorDataMessage.getPublisherID();
            val service = sensorDataMessage.getServiceName();


            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);

                if ((subscriptions.contains(
                        sensorDataMessage.getTopic())) ||
                        subscriptions.contains(Topic.allServicesOf(publisherId)) ||
                        subscriptions.contains(Topic.serviceTopic(publisherId, service)) ||
                        subscriptions.contains(Topic.allServicesOf2(publisherId)) ||
                        subscriptions.contains(Topic.serviceTopic()) ||
                        subscriptions.contains(Topic.serviceTopic2()) ||
                        subscriptions.contains(Topic.serviceTopic(service))) {
                    listener.onMessageArrived(sensorDataMessage);
                }

            }

        } else if (message.getClassName().equalsIgnoreCase(QueryMessage.class.getName())) {
            val sqm = (QueryMessage) Message.convertFromPayload(payload, QueryMessage.class);

            val topic1 = topic.split("/");
            String destiny = topic1[2];

            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);
                if (subscriptions.contains(Topic.allClientsTopic(destiny))) {
                    listener.onMessageArrived(sqm);
                }

            }
        } else if (message.getClassName().equalsIgnoreCase(QueryResponseMessage.class.getName())) {
            val sqrm = (QueryResponseMessage) Message.convertFromPayload(payload, QueryResponseMessage.class);

            // pega o ID do subscriber da resposta
            val topic1 = topic.split("/");
            val subscriberId = topic1[1];

            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);

                if (subscriptions.contains(Topic.queryResponseTopic(subscriberId))) {
                    listener.onMessageArrived(sqrm);
                }

            }

        } else if (message.getClassName().equalsIgnoreCase(EventQueryMessage.class.getName())) {
            val eqm = (EventQueryMessage) Message.convertFromPayload(payload, EventQueryMessage.class);

            val topic1 = topic.split("/");
            val destiny = topic1[2];

            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);
                if (subscriptions.contains(Topic.allClientsTopic(destiny))) {
                    listener.onMessageArrived(eqm);
                }

            }
        } else if (message.getClassName().equalsIgnoreCase(EventQueryResponseMessage.class.getName())) {
            val eqrm = (EventQueryResponseMessage) Message.convertFromPayload(payload, EventQueryResponseMessage.class);

            // pega o ID do subscriber da resposta
            val topic1 = topic.split("/");
            val subscriberId = topic1[1];

            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);

                if (subscriptions.contains(Topic.eventQueryResponseTopic(subscriberId))) {
                    listener.onMessageArrived(eqrm);
                }

            }

        } else if (message.getClassName().equalsIgnoreCase(ServiceInformationMessage.class.getName())) {
            val sim = (ServiceInformationMessage) Message.convertFromPayload(payload, ServiceInformationMessage.class);

            val publisherId = sim.getPublisherID();
            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);
                if (subscriptions.contains(Topic.serviceInformationTopic()) ||
                        subscriptions.contains(Topic.serviceInformationTopic(publisherId))) {
                    listener.onMessageArrived(sim);
                }
            }
        } else if (message.getClassName().equalsIgnoreCase(LivelinessMessage.class.getName())) {

            val publisherId = message.getPublisherID();

            val livelinessMessage = (LivelinessMessage) Message.convertFromPayload(payload, LivelinessMessage.class);

            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);

                if (subscriptions.contains(topic) ||
                        subscriptions.contains(Topic.livenessTopic(publisherId))
                        || subscriptions.contains(Topic.livenessTopic())) {
                    listener.onMessageArrived(livelinessMessage);
                }

            }

        } else if (message.getClassName().equalsIgnoreCase(ConnectionChangedStatusMessage.class.getName())) {

            val publisherId = message.getPublisherID();

            val connectionChangedStatusMessage = (ConnectionChangedStatusMessage) Message.convertFromPayload(payload, ConnectionChangedStatusMessage.class);

            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);

                if (subscriptions.contains(topic) ||
                        subscriptions.contains(Topic.connectionChangedStatusTopic(publisherId)) ||
                        subscriptions.contains(Topic.connectionChangedStatusTopic())) {
                    listener.onMessageArrived(connectionChangedStatusMessage);
                }

            }

        } else if (message.getClassName().equalsIgnoreCase(ObjectFoundMessage.class.getName())) {
            val objectFoundMessage = (ObjectFoundMessage) Message.convertFromPayload(payload, ObjectFoundMessage.class);
            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);

                if (subscriptions.contains(topic) || subscriptions.contains(Topic.objectFoundTopic())) {
                    listener.onMessageArrived(objectFoundMessage);
                }

            }

        } else if (message.getClassName().equalsIgnoreCase(ObjectConnectedMessage.class.getName())) {
            val objectConnectedMessage = (ObjectConnectedMessage) Message.convertFromPayload(payload, ObjectConnectedMessage.class);
            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);

                if (subscriptions.contains(topic) || subscriptions.contains(Topic.objectConnectedTopic())) {
                    listener.onMessageArrived(objectConnectedMessage);
                }

            }

        } else if (message.getClassName().equalsIgnoreCase(ObjectDisconnectedMessage.class.getName())) {
            val objectDisconnectedMessage = (ObjectDisconnectedMessage) Message.convertFromPayload(payload, ObjectDisconnectedMessage.class);
            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);

                if (subscriptions.contains(topic) || subscriptions.contains(Topic.objectDisconnectedTopic())) {
                    listener.onMessageArrived(objectDisconnectedMessage);
                }

            }

        } else if (message.getClassName().equalsIgnoreCase(ObjectDiscoveredMessage.class.getName())) {
            val objectDiscoveredMessage = (ObjectDiscoveredMessage) Message.convertFromPayload(payload, ObjectDiscoveredMessage.class);
            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);

                if (subscriptions.contains(topic) || subscriptions.contains(Topic.objectDiscoveredTopic())) {
                    listener.onMessageArrived(objectDiscoveredMessage);
                }

            }

        } else if (message.getClassName().equalsIgnoreCase(Message.class.getName())) {
            val genericMessage = (Message) Message.convertFromPayload(payload, Message.class);


            val publisherId = genericMessage.getPublisherID();
            val service = genericMessage.getServiceName();


            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);

                if ((subscriptions.contains(genericMessage.getTopic())) ||
                        subscriptions.contains(Topic.allServicesOf(publisherId)) ||
                        subscriptions.contains(Topic.serviceTopic(publisherId, service)) ||
                        subscriptions.contains(Topic.allServicesOf2(publisherId)) ||
                        subscriptions.contains(Topic.serviceTopic()) ||
                        subscriptions.contains(Topic.serviceTopic2()) ||
                        subscriptions.contains(Topic.serviceTopic(service))) {
                    listener.onMessageArrived(genericMessage);
                }

            }

        } else {
            val genericMessage = (Message) Message.convertFromPayload(payload, Message.class);


            val publisherId = genericMessage.getPublisherID();
            val service = genericMessage.getServiceName();


            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);

                if ((subscriptions.contains(genericMessage.getTopic())) ||
                        subscriptions.contains(Topic.allServicesOf(publisherId)) ||
                        subscriptions.contains(Topic.serviceTopic(publisherId, service)) ||
                        subscriptions.contains(Topic.allServicesOf2(publisherId)) ||
                        subscriptions.contains(Topic.serviceTopic()) ||
                        subscriptions.contains(Topic.serviceTopic2()) ||
                        subscriptions.contains(Topic.serviceTopic(service))) {
                    listener.onMessageArrived(genericMessage);
                }

            }

        /** } else {
            for (IMessageListener listener : listeners.keySet()) {

                val subscriptions = listeners.get(listener);

                if (subscriptions.contains(topic)) {
                    listener.onMessageArrived(message);
                }

            } **/
        }
    }

    private synchronized void registerListenerAndTopic(IMessageListener listener, String topic) {

        if (isTopicInvalid(topic)) return;

        if (!listeners.containsKey(listener)) {
            // registra listener se nao existe ainda
            listeners.put(listener, new ArrayList<String>());
        }

        // pega topicos
        val topics = listeners.get(listener);
        if (!topics.contains(topic)) {
            // se topico nao esta registrado, registra
            topics.add(topic);
        }

    }

    private synchronized void unregisterListenerAndTopic(IMessageListener listener, String topic) {

        if (isTopicInvalid(topic)) return;

        if (listeners.containsKey(listener)) {

            val topics = listeners.get(listener);
            if (topics.contains(topic)) {
                topics.remove(topic);
            }

            if (topics.size() == 0) {
                listeners.remove(listener);
            }

        }

    }

    private String getWebSocketPort() {
        return webSocketPort;
    }

    private void setWebSocketPort(String webSocketPort) {
        this.webSocketPort = webSocketPort;
    }

    private boolean isRequestDisconnect() {
        return requestDisconnect;
    }

    private void setRequestDisconnect(boolean requestDisconnect) {
        this.requestDisconnect = requestDisconnect;
    }

    private synchronized void unregisterAll() {
        listeners.clear();
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    private class DeliveredMessageActionLister implements IMqttActionListener {

        private final Message message;

        DeliveredMessageActionLister(Message message) {
            super();
            this.message = message;
        }

        @Override
        public synchronized void onFailure(IMqttToken arg0, Throwable arg1) {
            AppUtils.logger('e', TAG, ">>> Falha na entrada da mensagem: " + message);
            message.setDeliveredFailed(true);
            if (!cleanSession && message.getQos() > 0 && !deliveryFailedMessages.contains(message) && !message.isDelivered() && isEnableIntermediateBuffer()) {
                deliveryFailedMessages.add(message);
            }
        }

        @Override
        public synchronized void onSuccess(IMqttToken arg0) {
            messageDeliverySuccess++;
            //AppUtils.logger('i', TAG, ">> Message Delivery Success " + message);
            //AppUtils.logger('i', TAG, messageDeliverySuccess + " mensagens entregues com sucesso");
            message.setDelivered(true);
            message.setDeliveredFailed(false);
            if (deliveryFailedMessages.contains(message)) {
                deliveryFailedMessages.remove(message);
            }
            val publisherListener = message.getPublisherListener();
            if (publisherListener != null) {
                publisherListener.onMessageDelivered(message);
            }
            unlock();

        }


    }

    private class RepublishMessagesTimerTask extends TimerTask {

        @Override
        public void run() {
            tryAgain();
            republishMessagesTimerTask = new RepublishMessagesTimerTask();
            timer.schedule(republishMessagesTimerTask, republicationInterval);
            cancel();
        }
    }

    private synchronized void checkSubscriptionPendencies() {
        for (Subscription subscription : subscriptionPendencies) {
            val topic = subscription.getTopic();
            val reliability = subscription.getReliability();
            val action = subscription.getAction();
            val listener = subscription.getConnectionListener();
            if (action.equalsIgnoreCase("subscribe")) {
                subscribe(topic, reliability, listener);
            } else if (action.equalsIgnoreCase("unsubscribe")) {
                unsubscribe(topic, listener);
            }
        }
    }

    private synchronized void notifyConnectionSuccess() {
        for (IConnectionListener connectionListener : connectionListeners) {
            if (connectionListener != null) {
                connectionListener.onConnectionEstablished();
            }
        }
    }

    private synchronized void notifyDisconnection() {
        for (IConnectionListener connectionListener : connectionListeners) {
            if (connectionListener != null) {
                connectionListener.onDisconnectedNormally();
            }
        }
    }

    private synchronized void notifyConnectionFalied() {
        for (IConnectionListener connectionListener : connectionListeners) {
            if (connectionListener != null) {
                connectionListener.onConnectionEstablishmentFailed();
            }
        }
    }

    private synchronized void notifyConnectionLost() {
        for (IConnectionListener connectionListener : connectionListeners) {
            if (connectionListener != null) {
                connectionListener.onConnectionLost();
            }
        }
    }

    private synchronized void tryAgain() {

        if (isConnected()) {
            final ArrayList<Message> messages = new ArrayList<>();
            messages.addAll(deliveryFailedMessages);
            for (Message message : messages) {
                if (!cleanSession && message.getQos() > 0 && !message.isDelivered() && message.isDeliveredFailed() && isEnableIntermediateBuffer()) {
                    message.setDeliveredFailed(false);
                    republish(message);
                    timer.schedule(new CheckDeliveryTimerTask(message), retryInterval);
                }

            }
        }
    }

    private synchronized void unlock() {
        notifyAll();
    }

    private synchronized void lock() {
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isTopicInvalid(String topic) {
        return topic == null || topic.isEmpty() || topic.equalsIgnoreCase("null");
    }

    private class CheckDeliveryTimerTask extends TimerTask {

        private Message message;

        public CheckDeliveryTimerTask(Message message) {
            this.message = message;
        }

        @Override
        public void run() {

            if (!cleanSession && message.getQos() > 0 && !message.isDelivered() && isEnableIntermediateBuffer()) {

                message.setDeliveredFailed(true);
                if (!deliveryFailedMessages.contains(message)) {
                    deliveryFailedMessages.add(message);
                }
            }
            cancel();
        }
    }

    @Override
    public synchronized void reconnect() {
        if (!isConnected()) {
            try {
                Thread.sleep(automaticReconnectionTime);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            AppUtils.logger('i', TAG, ">>> Tentando reconectar ao Broker MQTT...");
            connect();


        }
    }

    private void publishConnectionStatusMessage(ConnectionChangedStatusMessage connectionChangedStatusMessage) {
        connectionChangedStatusMessage.setTopic(Topic.connectionChangedStatusTopic(clientId));
        connectionChangedStatusMessage.setQos(2);
        connectionChangedStatusMessage.setRetained(false);
        connectionChangedStatusMessage.setBrokerAddress(lastUri);
        publish(connectionChangedStatusMessage);
    }

    private synchronized void republish(Message message) {
        try {
            message.setPublicationTimestamp(Time.getCurrentTimestamp());
            val deliveredMessageActionLister = new DeliveredMessageActionLister(message);
            AppUtils.logger('i', TAG, ">>> Republicando mensagem: " + message);
            timer.schedule(new CheckDeliveryTimerTask(message), retryInterval);
            mqttClient.publish(message.getTopic(), message, null, deliveredMessageActionLister);
        } catch (Exception e) {
            message.setDeliveredFailed(true);
            if (!cleanSession && message.getQos() > 0 && !deliveryFailedMessages.contains(message) && isEnableIntermediateBuffer()) {
                deliveryFailedMessages.add(message);
            }
        }
    }

}


