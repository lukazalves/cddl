package br.ufma.lsdi.cddl.network;

import br.ufma.lsdi.cddl.listeners.IMessageListener;
import lombok.val;

/**
 * Created by bertodetacio on 27/05/17.
 */

public final class Subscription implements Comparable<Subscription> {
    private String topic;
    private int reliability;
    private String action;
    private IMessageListener connectionListener;


    public Subscription()  {}

    public Subscription(String topic, int reliability, String action, IMessageListener connectionListener) {
        this.topic = topic;
        this.reliability = reliability;
        this.action = action;
        this.connectionListener = connectionListener;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getReliability() {
        return reliability;
    }

    public void setReliability(int reliability) {
        this.reliability = reliability;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public IMessageListener getConnectionListener() {
        return connectionListener;
    }

    public void setConnectionListener(IMessageListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Subscription) {
            val other = (Subscription) object;
            return other.getTopic().equalsIgnoreCase(getTopic());
        }
        return false;
    }

    @Override
    public int compareTo(Subscription other) {
        if (other.getTopic().equalsIgnoreCase(getTopic())) {
            return 0;
        } else {
            return -1;
        }
    }
}
