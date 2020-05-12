package br.ufma.lsdi.cddl.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class EventQueryResponseMessage extends Message implements Serializable {

    private static final long serialVersionUID = -5433094800290949481L;

    private final EventQueryMessage eventQueryMessage;
    private final List<ServiceInformationMessage> serviceInformatioMessageList = new ArrayList<ServiceInformationMessage>();

    private String subscriberID;

    public EventQueryResponseMessage(EventQueryMessage eqm) {
        this.eventQueryMessage = eqm;
        setQocEvaluated(true);
    }

    public List<ServiceInformationMessage> getServiceInformationMessageList() {
        return serviceInformatioMessageList;
    }

    public EventQueryMessage getEventQueryMessage() {
        return eventQueryMessage;
    }

    public String getSubscriberID() {
        return subscriberID;
    }

    public void setSubscriberID(String subscriberID) {
        this.subscriberID = subscriberID;
    }

}
