package br.ufma.lsdi.cddl.pubsub;

import java.util.ArrayList;
import java.util.Vector;

import br.ufma.lsdi.cddl.Connection;
import br.ufma.lsdi.cddl.listeners.IMessageListener;
import br.ufma.lsdi.cddl.listeners.ISubscriberListener;
import br.ufma.lsdi.cddl.listeners.ISubscriberQoSListener;
import br.ufma.lsdi.cddl.message.ConnectionChangedStatusMessage;
import br.ufma.lsdi.cddl.message.EventQueryMessage;
import br.ufma.lsdi.cddl.message.EventQueryResponseMessage;
import br.ufma.lsdi.cddl.message.LivelinessMessage;
import br.ufma.lsdi.cddl.message.Message;
import br.ufma.lsdi.cddl.message.QueryMessage;
import br.ufma.lsdi.cddl.message.QueryResponseMessage;
import br.ufma.lsdi.cddl.message.ServiceInformationMessage;
import br.ufma.lsdi.cddl.qos.LatencyBudgetQoS;
import br.ufma.lsdi.cddl.qos.LifespanQoS;
import br.ufma.lsdi.cddl.util.Time;
import br.ufma.lsdi.cddl.util.Topic;
import lombok.val;


class SubscriberImpl extends ClientImpl implements Subscriber, IMessageListener {

    private static final String TAG = SubscriberImpl.class.getSimpleName();

    private final Vector<Message> latencyBudgestMessages = new Vector<>();

    private final ArrayList<String> subscriptions = new ArrayList<>();

    private ISubscriberListener subscriberListener;

    private ISubscriberQoSListener subscriberQoSListener;

    private boolean paused = false;

    private final String CLIENT_ID = "CLIENT_ID"; // will be substituted when connection subscribes the message

    public SubscriberImpl() {
        super();
    }

    public SubscriberImpl(ISubscriberListener subscriberListener) {
        super(subscriberListener);
        this.subscriberListener = subscriberListener;
    }

    public SubscriberImpl(ISubscriberQoSListener subscriberQoSListener) {
        super(subscriberQoSListener);
        this.subscriberQoSListener = subscriberQoSListener;
    }

    public SubscriberImpl(ISubscriberListener subscriberListener, ISubscriberQoSListener subscriberQoSListener) {
        super(subscriberListener, subscriberQoSListener);
        this.subscriberListener = subscriberListener;
        this.subscriberQoSListener = subscriberQoSListener;
    }

    @Override
    protected boolean isDeadlineMissed(long deadline) {
        val lastMessage = getLastDeadlineMessage();
        if (lastMessage == null || lastMessage.getReceptionTimestamp() > deadline) {
            return true;
        } else {
            setLastDeadlineMessage(null);
            return false;
        }
    }

    @Override
    protected boolean isLivelinessMissed(long leaseDuration) {
        val lastLivelinessMessage = getLastLivelinessMessage();
        val lastMessage = getLastMessage();

        if ((lastLivelinessMessage != null && lastLivelinessMessage.getReceptionTimestamp() < leaseDuration) || (lastMessage != null && lastMessage.getReceptionTimestamp() < leaseDuration)) {
            setLastLivelinessMessage(null);
            setLastMessage(null);
            return false;

        } else {
            return true;
        }
    }

    protected void on_message_avaliable(Message message) {

        eval_filter_and_send(message);

    }

    public void send(Message message, String topic) {

        if (!paused) {
            if (subscriberListener != null) {
                try {
                    subscriberListener.onMessageArrived(message);
                    if (monitor.getNumRules() > 0) monitor.messageArrived(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }


    @Override
    protected synchronized void on_message_received(Message message) {
        val latencyBudgetQoS = getLatencyBudgetQoS();
        if (latencyBudgetQoS.getDelay() == LatencyBudgetQoS.DEFAULT_DELAY) {
            message.setReceptionTimestamp(Time.getCurrentTimestamp());
            val history = getHistory();
            val lifeTime = getLifeTime(message, false);
            val lifespanQoS = getLifespanQoS();
            if (lifespanQoS.getExpirationTime() == LifespanQoS.INFINITE_DURATION || lifeTime > 0) {
                history.insert(message);
                startLisfespanClock(message, lifeTime);
            }
            setLastDeadlineMessage(message);
            setLastMessage(message);
            on_message_avaliable(message);
        } else {
            latencyBudgestMessages.add(message);
        }
    }

    @Override
    public synchronized void addConnection(Connection connection) {
        super.addConnection(connection);
        subscribeQueryResponseTopic();
        subscribeEventQueryResponseTopic();
    }

    protected synchronized void on_latency_budget_timer_finish() {
        val history = getHistory();
        val messagesToRemove = new Vector<Message>(latencyBudgestMessages);
        for (Message message : messagesToRemove) {
            message.setReceptionTimestamp(Time.getCurrentTimestamp());
            val lifeTime = getLifeTime(message, false);
            val lifespanQoS = getLifespanQoS();
            if (lifespanQoS.getExpirationTime() == LifespanQoS.INFINITE_DURATION || lifeTime > 0) {
                history.insert(message);
                startLisfespanClock(message, lifeTime);
            }
            on_message_avaliable(message);
        }
        if (!messagesToRemove.isEmpty()) {
            val lastMessage = messagesToRemove.get(messagesToRemove.size() - 1);
            setLastDeadlineMessage(lastMessage);
            setLastMessage(lastMessage);
        }
        latencyBudgestMessages.removeAll(messagesToRemove);
    }

    @Override
    public void onMessageArrived(Message message) {

        if (message == null) {
            return;
        }

        val acceptdRetained = getDurabilityQoS().isRetained();

        if (!acceptdRetained && message.isRetained()) {
            return;
        } else if (message instanceof LivelinessMessage) {
            setLastLivelinessMessage((LivelinessMessage) message);
            if (subscriberListener != null) subscriberListener.onMessageArrived(message);
        } else if (message instanceof ConnectionChangedStatusMessage) {
            clientQoSListener.onClientConnectionChangedStatus(message.getPublisherID(), ((ConnectionChangedStatusMessage) message).getStatus());
            if (subscriberListener != null) subscriberListener.onMessageArrived(message);

        } else if (message instanceof QueryMessage) {
            if (subscriberListener != null) subscriberListener.onMessageArrived(message);

        } else if (message instanceof QueryResponseMessage) {
            if (subscriberListener != null) subscriberListener.onMessageArrived(message);
        } else if (message instanceof EventQueryMessage) {
            if (subscriberListener != null) subscriberListener.onMessageArrived(message);
        } else if (message instanceof EventQueryResponseMessage) {
            if (subscriberListener != null) subscriberListener.onMessageArrived(message);
        } else if (message instanceof ServiceInformationMessage) {
            if (subscriberListener != null) subscriberListener.onMessageArrived(message);
        } else {
            addToQueue(message);
        }
    }

    //@Override
    //public void subscribeServiceTopic() {
    //    subscribe(Topic.serviceTopic());
    //}

    //@Override
    //public void unsubscribeServiceTopic() {
    //    unsubscribe(Topic.serviceTopic());
    //}

    @Override
    public void subscribeServiceByName(String serviceName) {
        subscribeTopic(Topic.serviceTopic(serviceName));
    }

    @Override
    public void unsubscribeServiceByName(String serviceName) {
        unsubscribeTopic(Topic.serviceTopic(serviceName));
    }

    @Override
    public void subscribeServiceByPublisherId(String publisherId) {
        subscribeTopic(Topic.allServicesOf(publisherId));
    }

    @Override
    public void unsubscribeServiceByPublisherId(String publisherId) {
        unsubscribeTopic(Topic.allServicesOf(publisherId));
    }

    @Override
    public void subscribeServiceByPublisherAndName(String publisherId, String serviceName) {
        val topic = Topic.serviceTopic(publisherId, serviceName);
        subscribeTopic(topic);
    }

    @Override
    public void unsubscribeServiceByPublisherAndName(String publisherId, String serviceName) {
        val topic = Topic.serviceTopic(publisherId, serviceName);
        unsubscribeTopic(topic);
    }

    @Override
    public void subscribeServiceByServiceInformationMessage(ServiceInformationMessage sim) {
        val topic = Topic.serviceTopic(sim.getPublisherID(), sim.getServiceName());
        subscribeTopic(topic);
    }

    @Override
    public void unsubscribeServiceByServiceInformationMessage(ServiceInformationMessage sim) {
        val topic = Topic.serviceTopic(sim.getPublisherID(), sim.getServiceName());
        unsubscribeTopic(topic);
    }

    @Override
    public void subscribeQueryTopic() {
        subscribeTopic(Topic.queryTopic());
    }

    @Override
    public void unsubscribeQueryTopic() {
        unsubscribeTopic(Topic.queryTopic());
    }

    @Override
    public void subscribeCancelQueryTopic() {
        subscribeTopic(Topic.cancelQueryTopic());
    }

    @Override
    public void unsubscribeCancelQueryTopic() {
        unsubscribeTopic(Topic.cancelQueryTopic());
    }

    @Override
    public void subscribeCommandTopic() {
        subscribeTopic(Topic.commandTopic());
    }

    @Override
    public void unsubscribeCommandTopic() {
        unsubscribeTopic(Topic.commandTopic());
    }

    @Override
    public void subscribeQueryResponseTopic() {
        val topic = Topic.queryResponseTopic(CLIENT_ID);
        subscribeTopic(topic);
    }

    public void unsubscribeQueryResponseTopic() {
        val topic = Topic.queryResponseTopic(CLIENT_ID);
        unsubscribeTopic(topic);
    }

    @Override
    public void subscribeEventQueryTopic() {
        subscribeTopic(Topic.eventQueryTopic());
    }

    @Override
    public void unsubscribeEventQueryTopic() {
        unsubscribeTopic(Topic.eventQueryTopic());
    }

    @Override
    public void subscribeEventQueryResponseTopic() {
        val topic = Topic.eventQueryResponseTopic(CLIENT_ID);
        subscribeTopic(topic);
    }

    @Override
    public void unsubscribeEventQueryResponseTopic() {
        val topic = Topic.eventQueryResponseTopic(CLIENT_ID);
        unsubscribeTopic(topic);
    }

    @Override
    public void subscribeEventQueryResponseTopicBySubscriberId(String subscriberId) {
        subscribeTopic(Topic.eventQueryResponseTopic(subscriberId));
    }

    @Override
    public void unsubscribeEventQueryResponseTopicBySunscriberId(String subscriberId, IMessageListener listener) {
        unsubscribeTopic(Topic.eventQueryResponseTopic(subscriberId));
    }

    @Override
    public void subscribeLivelenessTopicByPublisherId(String publisherId) {
        subscribeTopic(Topic.livenessTopic(publisherId));
    }

    @Override
    public void unsubscribeLivelenessTopicByPublisherId(String publisherId) {
        unsubscribeTopic(Topic.livenessTopic(publisherId));
    }

    @Override
    public void subscribeLivelenessTopic() {
        subscribeTopic(Topic.livenessTopic());
    }

    @Override
    public void unsubscribeLivelenessTopic() {
        unsubscribeTopic(Topic.livenessTopic());
    }

    @Override
    public void subscribeConnectionChangedStatusTopic(String publisherID) {
        subscribeTopic(Topic.connectionChangedStatusTopic(publisherID));
    }

    @Override
    public void unsubscribeConnectionChangedStatusTopic(String publisherID) {
        unsubscribeTopic(Topic.connectionChangedStatusTopic(publisherID));
    }

    @Override
    public void subscribeConnectionChangedStatusTopic() {
        subscribeTopic(Topic.connectionChangedStatusTopic());
    }

    @Override
    public void unsubscribeConnectionChangedStatusTopic() {
        unsubscribeTopic(Topic.connectionChangedStatusTopic());
    }

    @Override
    public synchronized void subscribeTopic(String topicName) {
        for (Connection connection : connections) {
            val reliability = getReliabilityQoS().getKind();
            connection.subscribe(topicName, reliability, this);
        }
        //EventBus.getDefault().post(new ConnectionServiceSubscribeMessage(topic, reliability, this));

    }

    @Override
    public synchronized void unsubscribeTopic(String topicName) {
        for (Connection connection : connections) {
            connection.unsubscribe(topicName, this);
        }
        //EventBus.getDefault().post(new ConnectionServiceUnsubscribeMessage(topic, this));
    }

    @Override
    public void pauseSubscriptions() {
        paused = true;
    }

    @Override
    public void resumeSubscriptions() {
        paused = false;
    }

    @Override
    public void unsubscribeAll() {
        for (Connection connection : connections) {
            connection.unsubscribeAll();
        }
    }

    @Override
    public void subscribeObjectFoundTopic() {
        subscribeTopic(Topic.objectFoundTopic());
    }

    @Override
    public void subscribeObjectConnectedTopic() {
        subscribeTopic(Topic.objectConnectedTopic());
    }

    @Override
    public void subscribeObjectDisconnectedTopic() {
        subscribeTopic(Topic.objectDisconnectedTopic());
    }

    @Override
    public void subscribeObjectDiscoveredTopic() {
        subscribeTopic(Topic.objectDiscoveredTopic());
    }

    @Override
    public ISubscriberListener getSubscriberListener() {
        return subscriberListener;
    }

    @Override
    public void setSubscriberListener(ISubscriberListener subscriberListener) {
        this.subscriberListener = subscriberListener;
        setClientListener(subscriberListener);
    }

    @Override
    public ISubscriberQoSListener getSubscriberQoSListener() {
        return subscriberQoSListener;
    }

    @Override
    public void setSubscriberQoSListener(ISubscriberQoSListener subscriberQoSListener) {
        this.subscriberQoSListener = subscriberQoSListener;
        setClientQoSListener(subscriberQoSListener);
    }
}

