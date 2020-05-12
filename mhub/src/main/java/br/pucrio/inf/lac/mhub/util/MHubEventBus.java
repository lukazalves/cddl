package br.pucrio.inf.lac.mhub.util;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

/**
 * Created by bertodetacio on 10/06/17.
 */

public class MHubEventBus extends EventBus {

    private static MHubEventBus instance;

    ArrayList<Object> events = new ArrayList<>();

    public static MHubEventBus getDefault() {
        if (instance == null) {
            instance = new MHubEventBus();
        }
        return instance;
    }

    public void postHistory(Object event) {
        if (!events.contains(event)) {
            events.add(event);
        }
        super.post(event);
    }

    public ArrayList<Object> getHistory() {
        return events;
    }

    @Override
    public synchronized void register(Object subscriber) {
        super.register(subscriber);
        for (Object event : events) {
            post(event);
        }
    }
}
