package br.ufma.lsdi.cddl.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.val;

public final class MessageGroup extends Message {

    private final ArrayList<Message> messagesQueue = new ArrayList<>();

    private boolean isHistoricalMessage = false;

    public MessageGroup() {
        Collections.synchronizedList(messagesQueue);
    }

    public MessageGroup(List<? extends Message> messages) {
        Collections.synchronizedList(messagesQueue);
        messagesQueue.addAll(messages);
        setQocEvaluated(true);
    }

    public void add(Message message) {
        messagesQueue.add(message);
    }

    public void remove(Message message) {
        messagesQueue.remove(message);
    }

    public void removeAll(ArrayList<Message> messages) {
        messagesQueue.removeAll(messages);
    }

    public Message getFirst() {
        if (messagesQueue.size() > 0) {
            return messagesQueue.get(0);
        }
        return null;
    }

    public Message takeFirst() {
        if (messagesQueue.size() > 0) {
            return messagesQueue.remove(0);
        }
        return null;
    }

    public Message getLast() {
        if (messagesQueue.size() > 0) {
            return messagesQueue.get(messagesQueue.size() - 1);
        }
        return null;
    }

    public Message takeLast() {
        if (messagesQueue.size() > 0) {
            return messagesQueue.remove(messagesQueue.size() - 1);
        }
        return null;
    }

    public ArrayList<Message> getAll() {
        val messages = new ArrayList<>(messagesQueue);
        Collections.synchronizedList(messages);
        return messages;
    }

    public ArrayList<Message> takeAll() {
        val messages = new ArrayList<>(messagesQueue);
        messagesQueue.clear();
        return messages;
    }

    public boolean isHistoricalMessage() {
        return isHistoricalMessage;
    }

    public void setHistoricalMessage(boolean isHistoricalMessage) {
        this.isHistoricalMessage = isHistoricalMessage;
    }

    public int getSize() {
        return messagesQueue.size();
    }

    public boolean isEmpty() {
        return messagesQueue.isEmpty();
    }

}
