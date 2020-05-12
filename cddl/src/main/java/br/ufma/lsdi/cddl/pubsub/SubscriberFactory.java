package br.ufma.lsdi.cddl.pubsub;

import br.ufma.lsdi.cddl.listeners.ISubscriberListener;
import br.ufma.lsdi.cddl.listeners.ISubscriberQoSListener;

/**
 * Created by bertodetacio on 17/05/17.
 */

public final class SubscriberFactory {

    private SubscriberFactory() {}

    /**
     * Creates and returns a new subscriber
     * @return a new subscriber
     */
    public static Subscriber createSubscriber() {
        return new SubscriberImpl();
    }

    /**
     * Creates and returns a new subscriber by specifying a listener.
     * @param subscriberListener Listener that will receive the subscribed messages
     * @return a new subscriber
     */
    public static Subscriber createSubscriber(ISubscriberListener subscriberListener) {
        return new SubscriberImpl(subscriberListener);
    }

    public static Subscriber createSubscriber(ISubscriberQoSListener subscriberQoSListener) {
        return new SubscriberImpl(subscriberQoSListener);
    }

    public static Subscriber createSubscriber(ISubscriberListener subscriberListener, ISubscriberQoSListener subscriberQoSListener) {
        return new SubscriberImpl(subscriberQoSListener);
    }

}
