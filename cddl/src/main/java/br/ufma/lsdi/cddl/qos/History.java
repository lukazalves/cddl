package br.ufma.lsdi.cddl.qos;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import br.ufma.lsdi.cddl.message.Message;
import lombok.val;

public final class History {

    private final Vector<Message> queueMessages = new Vector<>();

    private HistoryQoS historyQoS;

    private DestinationOrderQoS destinationOrderQoS;

    public History(HistoryQoS historyQoS, DestinationOrderQoS destinationOrderQoS) {
        this.historyQoS = historyQoS;
        this.destinationOrderQoS = destinationOrderQoS;
    }

    public synchronized boolean insert(Message message) {

        if (!contains(message)) {

            if (historyQoS.getKind() == HistoryQoS.KEEP_ALL) {

                queueMessages.add(message);

                Collections.sort(queueMessages, destinationOrderQoS);

                return true;

            } else if (historyQoS.getKind() == HistoryQoS.KEEP_LAST && historyQoS.getDepth() > 0) {

                this.queueMessages.add(message);

                Collections.sort(queueMessages, destinationOrderQoS);

                if (queueMessages.size() > 0 && queueMessages.size() > historyQoS.getDepth()) {
                    queueMessages.remove(0);

                    return true;
                }
            } else {
                return false;
            }

        }
        return false;
    }

    public synchronized Message read() {
        if (queueMessages.size() > 0) {
            return queueMessages.get(0);
        }
        return null;
    }

    public synchronized Message read(int index) {
        if (queueMessages.size() > 0) {
            return queueMessages.get(index);
        }
        return null;
    }

    public synchronized Message take() {
        if (queueMessages.size() > 0) {
            return queueMessages.remove(0);
        }
        return null;
    }

    public synchronized Message take(int index) {
        if (queueMessages.size() > index) {
            return queueMessages.remove(index);
        }
        return null;
    }

    public synchronized List<Message> readAll() {
        val messages = new ArrayList<Message>();
        messages.addAll(queueMessages);
        return messages;
    }

    public synchronized List<Message> takeAll() {
        val messages = new ArrayList<Message>();
        messages.addAll(queueMessages);
        queueMessages.clear();
        return messages;
    }

    public synchronized void remove(Message message) {
        if (queueMessages.contains(message)) {
            queueMessages.remove(message);
        }
    }

    public synchronized void remove(int index) {
        if (queueMessages.size() > index) {
            queueMessages.remove(index);
        }
    }

    public boolean contains(Message message) {
        return queueMessages.contains(message);
    }

    public boolean isEmpty() {
        return queueMessages.isEmpty();
    }

    public boolean isFull() {
        if (historyQoS.getKind() == HistoryQoS.KEEP_ALL) {
            return false;
        } else {
            return queueMessages.size() == historyQoS.getDepth();
        }
    }

    public synchronized void clear() {
        queueMessages.clear();
    }

    public int size() {
        return queueMessages.size();
    }

    public DestinationOrderQoS getDestinationOrderQoS() {
        return destinationOrderQoS;
    }

    public void setDestinationOrderQoS(DestinationOrderQoS destinationOrderQoS) {
        this.destinationOrderQoS = destinationOrderQoS;
    }

    public HistoryQoS getHistoryQoS() {
        return historyQoS;
    }

    public void setHistoryQoS(HistoryQoS historyQoS) {
        this.historyQoS = historyQoS;
    }

}
