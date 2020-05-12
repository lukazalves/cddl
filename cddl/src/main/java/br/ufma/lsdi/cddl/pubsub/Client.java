package br.ufma.lsdi.cddl.pubsub;

import br.ufma.lsdi.cddl.Connection;
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

/**
 * Created by lcmuniz on 05/03/17.
 */
public interface Client {

    ReliabilityQoS getReliabilityQoS();

    void setReliabilityQoS(ReliabilityQoS reliabilityQoS);

    DurabilityQoS getDurabilityQoS();

    void setDurabilityQoS(DurabilityQoS durabilityQoS);

    HistoryQoS getHistoryQoS();

    void setHistoryQoS(HistoryQoS historyQoS);

    History getHistory();

    void setHistory(History history);

    DeadlineQoS getDeadlineQoS();

    void setDeadlineQoS(DeadlineQoS deadlineQoS);

    TimeBasedFilterQoS getTimeBasedFilterQos();

    void setTimeBasedFilterQoS(TimeBasedFilterQoS timeBasedFilterQoS);

    LatencyBudgetQoS getLatencyBudgetQoS();

    void setLatencyBudgetQoS(LatencyBudgetQoS latencyBudgetQoS);

    LifespanQoS getLifespanQoS();

    void setLifespanQoS(LifespanQoS lifespanQoS);

    LivelinessQoS getLivelinessQoS();

    void setLivelinessQoS(LivelinessQoS livelinessQoS);

    DestinationOrderQoS getDestinationOrderQoS();

    void setDestinationOrderQoS(DestinationOrderQoS destinationOrderQoS);

    void setFilter(String eplFilter);

    void clearFilter();

    Monitor getMonitor();

    /**
     * Adds a connection to this subscriber
     * @param connection connection to be added to this subscriber
     */
    void addConnection(Connection connection);

    /**
     * Removes a connection from this subscriber
     * @param connection connection to be removed from this subscriber
     */
    void removeConnection(Connection connection);

    /**
     * Removes all connection from this subscriber
     */
    void removeAllConnections();

    void connectAll();

    void disconnectAll();
}
