package br.ufma.lsdi.cddl.pubsub;

import br.ufma.lsdi.cddl.listeners.IPublisherListener;
import br.ufma.lsdi.cddl.listeners.IPublisherQoSListener;
import br.ufma.lsdi.cddl.message.Message;
import br.ufma.lsdi.cddl.ontology.QueryType;

/**
 * Created by lcmuniz on 19/02/17.
 */
public interface Publisher extends Client {

    void publish(Message message);

    String query(QueryType queryType, String query);

    void cancelQuery(String returnCode);

    IPublisherListener getPublisherListener();

    IPublisherQoSListener getPublisherQoSListener();

    void setPublisherListener(IPublisherListener publisherListener);

    void setPublisherQoSListener(IPublisherQoSListener publisherQoSListener);

}
