package br.ufma.lsdi.cddl.pubsub;

import br.ufma.lsdi.cddl.listeners.IPublisherListener;
import br.ufma.lsdi.cddl.listeners.IPublisherQoSListener;

/**
 * Created by bertodetacio on 17/05/17.
 */

public final class PublisherFactory {

    private PublisherFactory() {}

    public static Publisher createPublisher() {
        return new PublisherImpl();
    }

    public static Publisher createPublisher(IPublisherListener publisherListener) {
        return new PublisherImpl(publisherListener);
    }

    public static Publisher createPublisher(IPublisherQoSListener publisherQoSListener) {
        return new PublisherImpl(publisherQoSListener);
    }

    public static Publisher createPublisher(IPublisherListener publisherListener, IPublisherQoSListener publisherQoSListener) {
        return new PublisherImpl(publisherQoSListener);
    }


}
