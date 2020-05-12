package br.ufma.lsdi.cddl.pubsub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import br.ufma.lsdi.cddl.Connection;
import br.ufma.lsdi.cddl.listeners.IClientListener;
import br.ufma.lsdi.cddl.listeners.IClientQoSListener;
import br.ufma.lsdi.cddl.message.CommandMessage;
import br.ufma.lsdi.cddl.message.ConnectionChangedStatusMessage;
import br.ufma.lsdi.cddl.message.EventQueryMessage;
import br.ufma.lsdi.cddl.message.EventQueryResponseMessage;
import br.ufma.lsdi.cddl.message.LivelinessMessage;
import br.ufma.lsdi.cddl.message.Message;
import br.ufma.lsdi.cddl.message.QueryMessage;
import br.ufma.lsdi.cddl.message.QueryResponseMessage;
import br.ufma.lsdi.cddl.message.SensorDataMessage;
import br.ufma.lsdi.cddl.message.ServiceInformationMessage;
import br.ufma.lsdi.cddl.qos.DeadlineQoS;
import br.ufma.lsdi.cddl.qos.DestinationOrderQoS;
import br.ufma.lsdi.cddl.qos.DurabilityQoS;
import br.ufma.lsdi.cddl.qos.History;
import br.ufma.lsdi.cddl.qos.HistoryQoS;
import br.ufma.lsdi.cddl.qos.LatencyBudgetQoS;
import br.ufma.lsdi.cddl.qos.LifespanQoS;
import br.ufma.lsdi.cddl.qos.LivelinessQoS;
import br.ufma.lsdi.cddl.qos.ReliabilityQoS;
import br.ufma.lsdi.cddl.qos.TimeBasedFilterQoS;
import br.ufma.lsdi.cddl.util.Time;
import lombok.val;

/**
 * Created by bertodetacio on 05/03/17.
 */
public abstract class ClientImpl extends Thread implements Client {

    private final Vector<LifespanTimerTask> lifespanTimerTasks = new Vector<>();

    private final ConcurrentHashMap<String, Message> timeBasedFilterMessagesMap = new ConcurrentHashMap<>();

    private DeadlineQoS deadlineQoS = new DeadlineQoS();

    private ReliabilityQoS reliabilityQoS = new ReliabilityQoS();

    private DurabilityQoS durabilityQoS = new DurabilityQoS();

    private HistoryQoS historyQoS = new HistoryQoS();

    private DestinationOrderQoS destinationOrderQoS = new DestinationOrderQoS();

    private History history = new History(historyQoS, destinationOrderQoS);

    private TimeBasedFilterQoS timeBasedFilterQoS = new TimeBasedFilterQoS();

    private LatencyBudgetQoS latencyBudgetQoS = new LatencyBudgetQoS();

    private LifespanQoS lifespanQoS = new LifespanQoS();

    private LivelinessQoS livelinessQoS = new LivelinessQoS();

    private DeadlineTimerTask deadlineTimerTask = null;

    private TimeBasedFilterTimerTask timeBasedFilterTimerTask = null;

    private LatencyBudgetTimerTask latencyBudgetTimerTask = null;

    private LivelinessTimerTask livelinessTimerTask = null;

    private Message lastDeadlineMessage = null;

    private LivelinessMessage lastLivelinessMessage = null;

    private Message lastMessage = null;

    protected final Filter filter = new FilterImpl(this);

    protected final Monitor monitor = new MonitorImpl();

    protected final Vector<Message> messageQueue = new Vector<>();

    protected final Timer timer = new Timer();

    protected final ArrayList<Connection> connections = new ArrayList<Connection>();

    protected IClientListener clientListener;

    protected IClientQoSListener clientQoSListener;

    public ClientImpl() {
        this.start();
    }

    public ClientImpl(IClientQoSListener clientQoSListener) {
        this.clientQoSListener = clientQoSListener;
        Collections.synchronizedList(connections);
        this.start();
    }

    public ClientImpl(IClientListener clientListener) {
        this.clientListener = clientListener;
        Collections.synchronizedList(connections);
        this.start();
    }

    public ClientImpl(IClientListener clientListener, IClientQoSListener clientQoSListener) {
        this.clientListener = clientListener;
        this.clientQoSListener = clientQoSListener;
        Collections.synchronizedList(connections);
        this.start();
    }

    protected abstract void on_message_received(Message message);

    protected abstract void on_latency_budget_timer_finish();

    protected abstract boolean isDeadlineMissed(long deadline);

    protected abstract boolean isLivelinessMissed(long leaseDuration);

    public void addToQueue(Message message) {

        if (getReliabilityQoS().getKind() == 2 && message.isDuplicate()) {
            return;
        }
        messageQueue.add(message);
        putToTimeBasedFilterMessagesMap(message);
        unlock();
    }

    @Override
    public void run() {
        while (true) {
            while (messageQueue.isEmpty()) {
                waiting();
            }
            removeMessagesFromQueue();
        }
    }

    public DurabilityQoS getDurabilityQoS() {
        return durabilityQoS;
    }

    public void setDurabilityQoS(DurabilityQoS durabilityQoS) {
        this.durabilityQoS = durabilityQoS;
    }

    public HistoryQoS getHistoryQoS() {
        return historyQoS;
    }

    public void setHistoryQoS(HistoryQoS historyQoS) {
        this.historyQoS = historyQoS;
        this.history.setHistoryQoS(historyQoS);
    }

    public History getHistory() {
        return history;
    }

    public void setHistory(History history) {
        this.history = history;
    }

    @Override
    public ReliabilityQoS getReliabilityQoS() {
        return this.reliabilityQoS;
    }

    @Override
    public void setReliabilityQoS(ReliabilityQoS reabilityQoS) {

        this.reliabilityQoS = reabilityQoS;

    }

    public DeadlineQoS getDeadlineQoS() {
        return deadlineQoS;
    }

    public void setDeadlineQoS(DeadlineQoS deadlineQoS) {

        val period = deadlineQoS.getPeriod();

        if (this.deadlineQoS.getPeriod() != period) {
            this.deadlineQoS = deadlineQoS;

            if (period == DeadlineQoS.DEFAULT_DEADLINE && deadlineTimerTask != null) {
                deadlineTimerTask.cancel();
            } else if (period > DeadlineQoS.DEFAULT_DEADLINE) {
                if (deadlineTimerTask != null) {
                    deadlineTimerTask.cancel();
                }
                deadlineTimerTask = new DeadlineTimerTask(Time.getCurrentTimestamp() + deadlineQoS.getPeriod());
                timer.scheduleAtFixedRate(deadlineTimerTask, period, period);
            }

        }
    }

    public TimeBasedFilterQoS getTimeBasedFilterQos() {
        return timeBasedFilterQoS;
    }

    public void setTimeBasedFilterQoS(TimeBasedFilterQoS timeBasedFilterQoS) {

        val minSeparation = timeBasedFilterQoS.getMinSeparation();

        if (this.timeBasedFilterQoS.getMinSeparation() != minSeparation) {
            this.timeBasedFilterQoS = timeBasedFilterQoS;

            if (minSeparation == TimeBasedFilterQoS.DEFAULT_MIN_SEPARATION_INTERVAL && timeBasedFilterTimerTask != null) {
                timeBasedFilterTimerTask.cancel();
            } else if (minSeparation > TimeBasedFilterQoS.DEFAULT_MIN_SEPARATION_INTERVAL) {
                if (timeBasedFilterTimerTask != null) {
                    timeBasedFilterTimerTask.cancel();
                }
                timeBasedFilterTimerTask = new TimeBasedFilterTimerTask();
                timer.scheduleAtFixedRate(timeBasedFilterTimerTask, minSeparation, minSeparation);
            }

        }

    }

    public LatencyBudgetQoS getLatencyBudgetQoS() {
        return latencyBudgetQoS;
    }

    public void setLatencyBudgetQoS(LatencyBudgetQoS latencyBudgetQoS) {

        val delay = latencyBudgetQoS.getDelay();

        if (this.latencyBudgetQoS.getDelay() != delay) {
            this.latencyBudgetQoS = latencyBudgetQoS;

            if (delay == LatencyBudgetQoS.DEFAULT_DELAY && latencyBudgetTimerTask != null) {
                latencyBudgetTimerTask.cancel();
            } else if (delay > LatencyBudgetQoS.DEFAULT_DELAY) {
                if (latencyBudgetTimerTask != null) {
                    latencyBudgetTimerTask.cancel();
                }
                latencyBudgetTimerTask = new LatencyBudgetTimerTask();
                timer.scheduleAtFixedRate(latencyBudgetTimerTask, delay, delay);
            }

        }
    }

    public LifespanQoS getLifespanQoS() {
        return lifespanQoS;
    }

    public void setLifespanQoS(LifespanQoS lifespanQoS) {

        val expirationTime = lifespanQoS.getExpirationTime();

        if (this.lifespanQoS.getExpirationTime() != expirationTime) {
            this.lifespanQoS = lifespanQoS;

            if (expirationTime == LifespanQoS.INFINITE_DURATION && !lifespanTimerTasks.isEmpty()) {
                for (LifespanTimerTask lifespanTimerTask : lifespanTimerTasks) {
                    lifespanTimerTask.cancel();
                }
            }
        }
    }


    public LivelinessQoS getLivelinessQoS() {
        return livelinessQoS;
    }

    @Override
    public void setLivelinessQoS(LivelinessQoS livelinessQoS) {

        if (this.livelinessQoS.getKind() == LivelinessQoS.MANUAL && livelinessQoS.getKind() == LivelinessQoS.AUTOMATIC && livelinessQoS.getLeaseDuration() > LivelinessQoS.DEFAULT_LEASE_DURANTION) {
            livelinessTimerTask = new LivelinessTimerTask(Time.getCurrentTimestamp() + livelinessQoS.getLeaseDuration());
            timer.scheduleAtFixedRate(livelinessTimerTask, livelinessQoS.getLeaseDuration(), livelinessQoS.getLeaseDuration());
        }

        if (this.livelinessQoS.getKind() == LivelinessQoS.AUTOMATIC && livelinessQoS.getKind() == LivelinessQoS.MANUAL && livelinessTimerTask != null) {
            livelinessTimerTask.cancel();
        } else if (this.livelinessQoS.getKind() == LivelinessQoS.AUTOMATIC && livelinessQoS.getKind() == LivelinessQoS.AUTOMATIC && this.livelinessQoS.getLeaseDuration() != livelinessQoS.getLeaseDuration() && livelinessTimerTask != null) {
            livelinessTimerTask.cancel();
            if (livelinessQoS.getLeaseDuration() > LivelinessQoS.DEFAULT_LEASE_DURANTION) {
                livelinessTimerTask = new LivelinessTimerTask(Time.getCurrentTimestamp() + livelinessQoS.getLeaseDuration());
                timer.scheduleAtFixedRate(livelinessTimerTask, livelinessQoS.getLeaseDuration(), livelinessQoS.getLeaseDuration());
            }
        }

        this.livelinessQoS = livelinessQoS;

    }

    public DestinationOrderQoS getDestinationOrderQoS() {
        return destinationOrderQoS;
    }

    @Override
    public void setDestinationOrderQoS(DestinationOrderQoS destinationOrderQoS) {
        this.destinationOrderQoS = destinationOrderQoS;
        this.history.setDestinationOrderQoS(destinationOrderQoS);
    }


    protected void putToTimeBasedFilterMessagesMap(Message message) {
        val topic = message.getTopic();
        if (!timeBasedFilterMessagesMap.containsKey(topic)) {
            timeBasedFilterMessagesMap.put(topic, message);
        }
    }

    protected synchronized void waiting() {
        try {
            wait();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected synchronized void unlock() {
        notifyAll();
    }

    public Message getLastDeadlineMessage() {
        return lastDeadlineMessage;
    }

    public void setLastDeadlineMessage(Message lastMessagePublished) {
        this.lastDeadlineMessage = lastMessagePublished;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(Message lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LivelinessMessage getLastLivelinessMessage() {
        return lastLivelinessMessage;
    }

    public void setLastLivelinessMessage(LivelinessMessage lastLivelinessMessage) {
        this.lastLivelinessMessage = lastLivelinessMessage;
    }


    protected void startLisfespanClock(Message message, long lifeTime) {
        if (lifeTime >= 0) {
            val lifespanTimerTask = new LifespanTimerTask(message);
            lifespanTimerTasks.add(lifespanTimerTask);
            timer.schedule(lifespanTimerTask, lifeTime);
        }
    }

    private void removeMessagesFromQueue() {
        val message = messageQueue.remove(0);
        if (message != null && timeBasedFilterQoS.getMinSeparation() == TimeBasedFilterQoS.DEFAULT_MIN_SEPARATION_INTERVAL) {
            on_message_received(message);
        }
    }


    protected void on_deadline_missed() {
        if (clientQoSListener != null) {
            clientQoSListener.onExpectedDeadlineMissed();
        }
    }

    protected void on_deadline_fulfilled() {
        if (clientQoSListener != null) {
            clientQoSListener.onExpectedDeadlineFulfilled();
        }
    }

    protected void on_liveliness_missed() {
        if (clientQoSListener != null) {
            clientQoSListener.onExpectedLivelinessMissed();
        }
    }

    protected void on_liveliness_fulfilled() {
        if (clientQoSListener != null) {
            clientQoSListener.onExpectedLivelinessFulfilled();
        }
    }

    protected void on_lifespan_message_expired(Message message) {
        if (clientQoSListener != null) {
            clientQoSListener.onLifespanExpired(message);
        }
    }

    public long getLifeTime(Message message, boolean publisher) {

        val lifespanQoS = getLifespanQoS();

        long lifeTime = 0;
        long expirationTime;

        if (lifespanQoS.getExpirationTime() != LifespanQoS.INFINITE_DURATION) {
            if (lifespanQoS.getExpirationTime() <= LifespanQoS.EXPIRATION_TIME_FROM_MESSAGE) {
                expirationTime = message.getExpirationTime();
            } else {
                expirationTime = lifespanQoS.getExpirationTime();
            }
            if (lifespanQoS.getTimestampKind() != LifespanQoS.RECEPTION_TIMESTAMP_KIND) {
                long timestamp = 0;
                if (lifespanQoS.getTimestampKind() == LifespanQoS.MENSUREMENT_TIMESTAMP_KIND && message instanceof SensorDataMessage) {
                    timestamp = ((SensorDataMessage) message).getMeasurementTime();
                } else if (lifespanQoS.getTimestampKind() == LifespanQoS.PUBLICATION_TIMESTAMP_KIND) {
                    if (publisher) {
                        lifeTime = expirationTime;
                        return lifeTime;
                    } else {
                        timestamp = message.getPublicationTimestamp();
                    }
                }
                long currentTimestamp = Time.getCurrentTimestamp();
                long differenceTime = currentTimestamp - timestamp;
                /*System.out.println("expiration time = "+expirationTime);
				System.out.println("base timestamp = "+timestamp);
				System.out.println("current timestamp = "+currentTimestamp);
				System.out.println("diference time = "+differenceTime);*/
                if (differenceTime >= 0) {
                    lifeTime = expirationTime - differenceTime;
                    if (lifeTime <= 0) {
                        //descomentar se quiser ser notificado de mensagens que expiraram antes de serem inseridas
                        //no cache do subscritor, ou seja, chegaram mortas
                        on_lifespan_message_expired(message);
                    }
                } else {
                    //indica que o recebedor está com relógio atrasado em relação ao publicador
                    //duas opções: considerar o tempo da recepção (default) ou considerar a mensagem como expirada
                    if (lifespanQoS.isEarlyClocks()) {
                        lifeTime = expirationTime;
                    } else {
                        lifeTime = -1;
                    }
                }
            } else {
                lifeTime = expirationTime;

            }
        }


        return lifeTime;
    }

    protected synchronized void setClientQoSListener(IClientQoSListener clientQoSListener) {
        this.clientQoSListener = clientQoSListener;
    }

    protected synchronized void setClientListener(IClientListener clientListener) {
        this.clientListener = clientListener;
    }


    protected abstract void send(Message message, String topic);

    private class DeadlineTimerTask extends TimerTask {

        private long deadline;

        public DeadlineTimerTask(long deadline) {
            super();
            this.deadline = deadline;
        }

        @Override
        public void run() {
            if (isDeadlineMissed(deadline)) {
                on_deadline_missed();
            } else {
                on_deadline_fulfilled();
            }
            deadline = deadline + deadlineQoS.getPeriod();
        }

    }

    private class TimeBasedFilterTimerTask extends TimerTask {
        @Override
        public void run() {
            val currentMessages = timeBasedFilterMessagesMap.values();
            for (Message message : currentMessages) {
                on_message_received(message);
                currentMessages.remove(message);
            }
        }
    }

    private class LatencyBudgetTimerTask extends TimerTask {
        @Override
        public void run() {
            on_latency_budget_timer_finish();
        }
    }

    private final class LifespanTimerTask extends TimerTask {

        private final Message message;
        private final LifespanQoS lifespanQoS = getLifespanQoS();
        private final ReliabilityQoS reliabilityQoS = getReliabilityQoS();

        public LifespanTimerTask(Message message) {
            super();
            this.message = message;
        }

        @Override
        public void run() {
            if (lifespanQoS.getExpirationTime() != LifespanQoS.INFINITE_DURATION) {
                val history = getHistory();
                history.remove(message);
                on_lifespan_message_expired(message);
            }
        }
    }

    private final class LivelinessTimerTask extends TimerTask {

        private long leaseDuration;

        public LivelinessTimerTask(long leaseDuration) {
            super();
            this.leaseDuration = leaseDuration;
        }

        @Override
        public void run() {

            if (isLivelinessMissed(leaseDuration)) {
                on_liveliness_missed();
            } else {
                on_liveliness_fulfilled();
            }
            leaseDuration = leaseDuration + livelinessQoS.getLeaseDuration();
        }

    }

    @Override
    public void setFilter(String eplFilter) {
        filter.set(eplFilter);
    }

    protected void eval_filter_and_send(Message message) {

        if (message instanceof ConnectionChangedStatusMessage) {
            send(message, message.getTopic());
        } else if (message instanceof LivelinessMessage) {
            send(message, message.getTopic());
        } else if (message instanceof QueryMessage) {
            send(message, message.getTopic());
        } else if (message instanceof QueryResponseMessage) {
            send(message, message.getTopic());
        } else if (message instanceof CommandMessage) {
            send(message, message.getTopic());
        } else if (message instanceof EventQueryMessage) {
            send(message, message.getTopic());
        } else if (message instanceof EventQueryResponseMessage) {
            send(message, message.getTopic());
        } else if (message instanceof ServiceInformationMessage) {
            send(message, message.getTopic());
        } else {
            if (filter.isSet()) {
                filter.process(message);
            } else {
                send(message, message.getTopic());
            }
        }
    }

    public void clearFilter() {
        filter.clear();
    }

    @Override
    public Monitor getMonitor() {
        return monitor;
    }

    public synchronized void addConnection(Connection connection) {
        if (!connections.contains(connection)) {
            connections.add(connection);
        }
    }

    public synchronized void removeConnection(Connection connection) {
        if (connections.contains(connection)) {
            connections.remove(connection);
        }

    }

    public synchronized void removeAllConnections() {
        connections.clear();

    }

    public synchronized void connectAll() {
        for (Connection connection : connections) {
            connection.connect();
        }
    }

    public synchronized void disconnectAll() {
        for (Connection connection : connections) {
            connection.disconnect();
        }
    }

}
