package br.ufma.lsdi.cddl.pubsub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.UUID;

import br.ufma.lsdi.cddl.Connection;
import br.ufma.lsdi.cddl.listeners.IPublisherListener;
import br.ufma.lsdi.cddl.listeners.IPublisherQoSListener;
import br.ufma.lsdi.cddl.message.CancelQueryMessage;
import br.ufma.lsdi.cddl.message.CommandMessage;
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
import br.ufma.lsdi.cddl.ontology.QueryType;
import br.ufma.lsdi.cddl.qos.LatencyBudgetQoS;
import br.ufma.lsdi.cddl.qos.LifespanQoS;
import br.ufma.lsdi.cddl.qos.LivelinessQoS;
import br.ufma.lsdi.cddl.util.CDDLEventBus;
import br.ufma.lsdi.cddl.util.Time;
import br.ufma.lsdi.cddl.util.Topic;
import lombok.val;


class PublisherImpl extends ClientImpl implements Publisher {

    private static final String TAG = PublisherImpl.class.getSimpleName();

    private final HashMap<String, MessageGroup> messageGroupMap = new HashMap<>();

    private final ArrayList<String> keys = new ArrayList<>();

    private AssertLivelinessTimerTask assertLivelinessTimerTask = null;

    private IPublisherListener publisherListener;

    private IPublisherQoSListener publisherQoSListener;

    private final String CLIENT_ID = "CLIENT_ID"; // will be substituted when connection publishes the message

    protected PublisherImpl() {
        super();
        Collections.synchronizedMap(messageGroupMap);
        Collections.synchronizedCollection(messageGroupMap.values());
    }

    protected PublisherImpl(IPublisherListener publisherListener, IPublisherQoSListener publisherQoSListener) {
        super(publisherListener, publisherQoSListener);
        this.publisherListener = publisherListener;
        this.publisherQoSListener = publisherQoSListener;
        Collections.synchronizedMap(messageGroupMap);
        Collections.synchronizedCollection(messageGroupMap.values());
    }

    protected PublisherImpl(IPublisherListener publisherListener) {
        super(publisherListener);
        Collections.synchronizedMap(messageGroupMap);
        Collections.synchronizedCollection(messageGroupMap.values());
    }

    protected PublisherImpl(IPublisherQoSListener publisherQoSListener) {
        super(publisherQoSListener);
        this.publisherQoSListener = publisherQoSListener;
        Collections.synchronizedMap(messageGroupMap);
        Collections.synchronizedCollection(messageGroupMap.values());
    }

    protected boolean isDeadlineMissed(long deadline) {
        val lastDeadlineMessage = getLastDeadlineMessage();
        if (lastDeadlineMessage == null || lastDeadlineMessage.getPublicationTimestamp() > deadline) {
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
        if ((lastLivelinessMessage != null && lastLivelinessMessage.getPublicationTimestamp() < leaseDuration) || (lastMessage != null && lastMessage.getPublicationTimestamp() < leaseDuration)) {
            setLastLivelinessMessage(null);
            setLastMessage(null);
            return false;

        } else {
            return true;
        }
    }

    @Override
    public void setLivelinessQoS(LivelinessQoS livelinessQoS) {

        val currentLivelinessQoS = getLivelinessQoS();

        val nextLeaseDuration = 1 + (int) (livelinessQoS.getLeaseDuration() * Math.random());

        if (currentLivelinessQoS.getKind() == LivelinessQoS.MANUAL && livelinessQoS.getKind() == LivelinessQoS.AUTOMATIC && livelinessQoS.getLeaseDuration() > LivelinessQoS.DEFAULT_LEASE_DURANTION) {
            assertLivelinessTimerTask = new AssertLivelinessTimerTask();
            timer.scheduleAtFixedRate(assertLivelinessTimerTask, nextLeaseDuration, nextLeaseDuration);
        } else if (currentLivelinessQoS.getKind() == LivelinessQoS.AUTOMATIC && livelinessQoS.getKind() == LivelinessQoS.MANUAL) {
            assertLivelinessTimerTask.cancel();
        } else if (currentLivelinessQoS.getKind() == LivelinessQoS.AUTOMATIC && livelinessQoS.getKind() == LivelinessQoS.AUTOMATIC && currentLivelinessQoS.getLeaseDuration() != livelinessQoS.getLeaseDuration() && assertLivelinessTimerTask != null) {
            assertLivelinessTimerTask.cancel();
            if (livelinessQoS.getLeaseDuration() > LivelinessQoS.DEFAULT_LEASE_DURANTION) {
                timer.scheduleAtFixedRate(assertLivelinessTimerTask, nextLeaseDuration, nextLeaseDuration);
            }
        }

        super.setLivelinessQoS(livelinessQoS);
    }

    private void assertLiveliness() {
        //  count++;
        //  if(count<=100) {
        val livelinessMessage = new LivelinessMessage();
        setLastLivelinessMessage(livelinessMessage);
        publishLivelinessMessage(livelinessMessage);
        //  }
    }


    private final class AssertLivelinessTimerTask extends TimerTask {

        AssertLivelinessTimerTask() {}

        @Override
        public void run() {
            assertLiveliness();
        }

    }

    private void checkLifespanAndPublish(Message message) {
        val lifespanQoS = getLifespanQoS();
        val history = getHistory();
        message.setPublicationTimestamp(Time.getCurrentTimestamp());
        val lifeTime = getLifeTime(message, true);
        if (lifespanQoS.getExpirationTime() == LifespanQoS.INFINITE_DURATION || lifeTime > 0) {
            message.setExpirationTime(lifeTime);
            history.insert(message);
            startLisfespanClock(message, lifeTime);
            setLastDeadlineMessage(message);
            setLastMessage(message);
            publishMessage(message);
        }
    }

    private void checkLifespanAndPublish(MessageGroup messageGroup) {
        val lifespanQoS = getLifespanQoS();
        val history = getHistory();
        messageGroup.setPublicationTimestamp(Time.getCurrentTimestamp());
        val messagesToRemove = new ArrayList<Message>();
        for (Message submessage : messageGroup.getAll()) {
            val lifeTime = getLifeTime(messageGroup, true);
            if (lifespanQoS.getExpirationTime() == LifespanQoS.INFINITE_DURATION || lifeTime > 0) {
                submessage.setExpirationTime(lifeTime);
                history.insert(messageGroup);
                setLastDeadlineMessage(messageGroup);
                setLastMessage(messageGroup);
                startLisfespanClock(submessage, lifeTime);
            } else {
                messagesToRemove.add(submessage);
            }
        }
        messageGroup.removeAll(messagesToRemove);
        if (!messageGroup.isEmpty()) {
            publishMessage(messageGroup);
        }
    }

    @Override
    protected synchronized void on_message_received(Message message) {
        val topic = message.getTopic();
        message.setReceptionTimestamp(Time.getCurrentTimestamp());
        val latencyBudgetQoS = getLatencyBudgetQoS();
        if (latencyBudgetQoS.getDelay() == LatencyBudgetQoS.DEFAULT_DELAY) {
            checkLifespanAndPublish(message);
        } else {
            if (!messageGroupMap.containsKey(topic)) {
                messageGroupMap.put(topic, new MessageGroup());
            }
            val messageGroup = messageGroupMap.get(topic);
            messageGroup.setTopic(topic);
            messageGroup.add(message);
        }
    }

    @Override
    protected synchronized void on_latency_budget_timer_finish() {
        val messages = messageGroupMap.values();
        val messagesToRemove = new ArrayList<>();
        synchronized (messages) {
            for (MessageGroup messageGroup : messages) {
                checkLifespanAndPublish(messageGroup);
                messagesToRemove.add(messageGroup);
            }
        }
        if (!messagesToRemove.isEmpty()) {
            messageGroupMap.values().removeAll(messagesToRemove);
        }

    }

    @Override
    public String query(QueryType queryType, String query) {

        val returnCode = UUID.randomUUID().toString();

        val queryMessage = new QueryMessage(query, queryType, returnCode);
        publishQueryMessage(queryMessage);

        return returnCode;

    }

    @Override
    public void cancelQuery(String returnCode) {
        val cancelQueryMessage = new CancelQueryMessage(returnCode);
        CDDLEventBus.getDefault().post(cancelQueryMessage);
    }

    @Override
    public void publish(Message message) {
        if (message == null) {
            return;
        } else if (message instanceof ObjectConnectedMessage) {
            val ocm = (ObjectConnectedMessage) message;
            publishObjectConnectedMessage(ocm);
        } else if (message instanceof ObjectDisconnectedMessage) {
            val odm = (ObjectDisconnectedMessage) message;
            publishObjectDisconnectedMessage(odm);
        } else if (message instanceof ObjectFoundMessage) {
            val ofm = (ObjectFoundMessage) message;
            publishObjectFoundMessage(ofm);
        } else if (message instanceof ObjectDiscoveredMessage) {
            val odm = (ObjectDiscoveredMessage) message;
            publishObjectDiscoveredMessage(odm);
        } else if (message instanceof LivelinessMessage) {
            val livelinessMessage = (LivelinessMessage) message;
            publishLivelinessMessage(livelinessMessage);
        } else if (message instanceof QueryMessage) {
            val queryMessage = (QueryMessage) message;
            publishQueryMessage(queryMessage);
        } else if (message instanceof CancelQueryMessage) {
            val cancelQueryMessage = (CancelQueryMessage) message;
            publishCancelQueryMessage(cancelQueryMessage);
        } else if (message instanceof CommandMessage) {
            val commandMessage = (CommandMessage) message;
            publishCommandMessage(commandMessage);
        } else if (message instanceof QueryResponseMessage) {
            val queryResponseMessage = (QueryResponseMessage) message;
            publishQueryResponseMessage(queryResponseMessage);
        } else if (message instanceof EventQueryMessage) {
            val eventQueryMessage = (EventQueryMessage) message;
            publishEventQueryMessage(eventQueryMessage);
        } else if (message instanceof EventQueryResponseMessage) {
            val eventQueryResponseMessage = (EventQueryResponseMessage) message;
            publishEventQueryResponseMessage(eventQueryResponseMessage);
        } else if (message instanceof ServiceInformationMessage) {
            val serviceInformationMessage = (ServiceInformationMessage) message;
            publishServiceInformationMessage(serviceInformationMessage);
        } else if (message instanceof ConnectionChangedStatusMessage) {
            return;
        } else if (message instanceof SensorDataMessage) {
            val sensorDataMessage = (SensorDataMessage) message;
            if (!message.isQocEvaluated()) {
                CDDLEventBus.getDefault().postSticky(sensorDataMessage);
            } else {
                publishSensorDataMessage(sensorDataMessage);
            }
        } else {

            if (!message.isQocEvaluated()) {
                message.publisher = this;
                CDDLEventBus.getDefault().postSticky(message);
            } else {
                String topic = message.getTopic();
                if (topic == null || topic.equalsIgnoreCase("")) {
                    topic = Topic.serviceTopic(CLIENT_ID, message.getServiceName());
                    message.setTopic(topic);
                }
                addToQueue(message);
            }
        }

    }

    private void publishSensorDataMessage(SensorDataMessage sensorDataMessage) {
        if (sensorDataMessage.getTopic() == null || sensorDataMessage.getTopic().equalsIgnoreCase("")) {
            val topic = Topic.serviceTopic(CLIENT_ID, sensorDataMessage.getServiceName());
            sensorDataMessage.setTopic(topic);
        }
        addToQueue(sensorDataMessage);
    }

    private void publishObjectFoundMessage(ObjectFoundMessage ofm) {
        if (ofm.getTopic() == null || ofm.getTopic().equalsIgnoreCase("")) {
            val topic = Topic.objectFoundTopic(CLIENT_ID);
            ofm.setTopic(topic);
        }
        publishMessage(ofm);
    }

    private void publishObjectConnectedMessage(ObjectConnectedMessage ocm) {
        if (ocm.getTopic() == null || ocm.getTopic().equalsIgnoreCase("")) {
            val topic = Topic.objectConnectedTopic(CLIENT_ID);
            ocm.setTopic(topic);
        }
        publishMessage(ocm);
    }

    private void publishObjectDisconnectedMessage(ObjectDisconnectedMessage odm) {
        if (odm.getTopic() == null || odm.getTopic().equalsIgnoreCase("")) {
            val topic = Topic.objectDisconnectedTopic(CLIENT_ID);
            odm.setTopic(topic);
        }
        publishMessage(odm);
    }

    private void publishObjectDiscoveredMessage(ObjectDiscoveredMessage odm) {
        if (odm.getTopic() == null || odm.getTopic().equalsIgnoreCase("")) {
            val topic = Topic.objectDiscoveredTopic(CLIENT_ID);
            odm.setTopic(topic);
        }
        publishMessage(odm);
    }

    private void publishLivelinessMessage(LivelinessMessage livelinessMessage) {
        if (livelinessMessage.getTopic() == null || livelinessMessage.getTopic().equalsIgnoreCase("")) {
            val topic = Topic.livenessTopic(CLIENT_ID);
            livelinessMessage.setTopic(topic);
        }
        publishMessage(livelinessMessage);
    }

    private void publishQueryMessage(QueryMessage queryMessage) {
        if (queryMessage.getTopic() == null || queryMessage.getTopic().equalsIgnoreCase("")) {
            val topic = Topic.queryTopic(CLIENT_ID);
            queryMessage.setTopic(topic);
        }

        // publish to local
        //EventBus.getDefault().post(queryMessage);

        // publish to global
        publishMessage(queryMessage);

    }

    private void publishCancelQueryMessage(CancelQueryMessage cancelQueryMessage) {
        if (cancelQueryMessage.getTopic() == null || cancelQueryMessage.getTopic().equalsIgnoreCase("")) {
            val topic = Topic.queryTopic(CLIENT_ID);
            cancelQueryMessage.setTopic(topic);
        }

        // publish to local
        //EventBus.getDefault().post(queryMessage);

        // publish to global
        publishMessage(cancelQueryMessage);

    }


    private void publishCommandMessage(CommandMessage commandMessage) {
        if (commandMessage.getTopic() == null || commandMessage.getTopic().equalsIgnoreCase("")) {
            val topic = Topic.commandTopic(CLIENT_ID);
            commandMessage.setTopic(topic);
        }
        // publish to local
        //EventBus.getDefault().post(commandMessage);
        // publish to global
        publishMessage(commandMessage);
    }

    private void publishQueryResponseMessage(QueryResponseMessage queryResponseMessage) {
        if  (queryResponseMessage.getTopic() == null || queryResponseMessage.getTopic().equalsIgnoreCase("")) {
            val topic = Topic.queryResponseTopic(queryResponseMessage.getSubscriberID());
            queryResponseMessage.setTopic(topic);
        }
        publishMessage(queryResponseMessage);
    }

    private void publishEventQueryMessage(EventQueryMessage eventQueryMessage) {
        if (eventQueryMessage.getTopic() == null || eventQueryMessage.getTopic().equalsIgnoreCase("")) {
            val topic = Topic.eventQueryTopic(CLIENT_ID);
            eventQueryMessage.setTopic(topic);
        }
        publishMessage(eventQueryMessage);
    }


    private void publishEventQueryResponseMessage(EventQueryResponseMessage eventQueryResponseMessage) {
        if (eventQueryResponseMessage.getTopic() == null || eventQueryResponseMessage.getTopic().equalsIgnoreCase("")) {
            val topic = Topic.eventQueryResponseTopic(eventQueryResponseMessage.getSubscriberID());
            eventQueryResponseMessage.setTopic(topic);
        }
        publishMessage(eventQueryResponseMessage);
    }

    private void publishServiceInformationMessage(ServiceInformationMessage serviceInformationMessage) {
        if (serviceInformationMessage.getTopic() == null || serviceInformationMessage.getTopic().equalsIgnoreCase("")) {
            val topic = Topic.serviceInformationTopic(CLIENT_ID);
            serviceInformationMessage.setTopic(topic);
        }
        publishMessage(serviceInformationMessage);
    }

    private void publishMessage(Message message) {
        eval_filter_and_send(message);
    }

    @Override
    protected synchronized void send(Message message, String topic) {
        val reliability = getReliabilityQoS().getKind();
        val retained = getDurabilityQoS().isRetained();
        message.setPublisherListener(publisherListener);

        message.setRetained(retained);
        message.setQos(reliability);
        message.setTopic(topic);
        for (Connection connection : connections) {
            connection.publish(message);
        }
        if (monitor.getNumRules() > 0) monitor.messageArrived(message);

    }

    private void publishMessage(MessageGroup messageGroup) {
        for (Message message : messageGroup.getAll()) {
            val reliability = getReliabilityQoS().getKind();
            val retained = getDurabilityQoS().isRetained();
            message.setPublicationTimestamp(Time.getCurrentTimestamp());
            message.setPublisherID(CLIENT_ID);
            message.setRetained(retained);
            message.setQos(reliability);
            message.toJson();
        }
        publishMessage((Message) messageGroup);
    }


    public IPublisherListener getPublisherListener() {
        return publisherListener;
    }

    public IPublisherQoSListener getPublisherQoSListener() {
        return publisherQoSListener;
    }

    public void setPublisherListener(IPublisherListener publisherListener) {
        this.publisherListener = publisherListener;
        setClientListener(publisherListener);
    }

    public void setPublisherQoSListener(IPublisherQoSListener publisherQoSListener) {
        this.publisherQoSListener = publisherQoSListener;
        setClientQoSListener(publisherQoSListener);
    }

}
