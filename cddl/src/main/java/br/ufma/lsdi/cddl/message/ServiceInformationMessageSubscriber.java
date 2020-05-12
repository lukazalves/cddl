package br.ufma.lsdi.cddl.message;


import com.espertech.esper.client.EPStatement;

import java.util.Collection;
import java.util.Map;

import br.ufma.lsdi.cddl.ontology.QueryType;
import br.ufma.lsdi.cddl.pubsub.Publisher;

public class ServiceInformationMessageSubscriber {

    private final Publisher publisher;
    private final QueryMessage queryMessage;
    private final EPStatement queryEventStatement;

    public ServiceInformationMessageSubscriber(Publisher publisher, QueryMessage queryMessage, EPStatement queryEventStatement) {
        this.publisher = publisher;
        this.queryMessage = queryMessage;
        this.queryEventStatement = queryEventStatement;
    }

    public void update(Map[] map, Map[] removed) {
        QueryResponseMessage qrm = new QueryResponseMessage(queryMessage);
        qrm.setSubscriberID(queryMessage.getPublisherID());

        for (Map m : map) {

            Collection c = m.values();

            for (Object o : c) {
                Map e = (Map) o;

                ServiceInformationMessage sim = new ServiceInformationMessage();
                sim.setPublisherID(e.get("publisherID") == null ? "" : (String) e.get("publisherID"));
                sim.setServiceName(e.get("serviceName") == null ? "" : (String) e.get("serviceName"));
                sim.setAccuracy(e.get("accuracy") == null ? null : (Double) e.get("accuracy"));
                sim.setMeasurementTime(e.get("measurementTime") == null ? null : (Long) e.get("measurementTime"));
                sim.setAvailableAttributes(e.get("availableAttributes") == null ? null : (Integer) e.get("availableAttributes"));
                sim.setSourceLocationLatitude(e.get("sourceLocationLatitude") == null ? null : (Double) e.get("sourceLocationLatitude"));
                sim.setSourceLocationLongitude(e.get("sourceLocationLongitude") == null ? null : (Double) e.get("sourceLocationLongitude"));
                sim.setSourceLocationAltitude(e.get("sourceLocationAltitude") == null ? null : (Double) e.get("sourceLocationAltitude"));
                sim.setSourceLocationAltitude(e.get("sourceLocationAccuracy") == null ? null : (Double) e.get("sourceLocationAccuracy"));
                sim.setSourceLocationTimestamp(e.get("sourceLocationTimestamp") == null ? null : (Long) e.get("sourceLocationTimestamp"));
                sim.setMeasurementInterval(e.get("measurementInterval") == null ? null : (Long) e.get("measurementInterval"));
                sim.setNumericalResolution(e.get("numericalResolution") == null ? null : (Integer) e.get("numericalResolution"));
                qrm.getServiceInformationMessageList().add(sim);

            }


        }

        publisher.publish(qrm);

        if (queryMessage.getType() == QueryType.INSTANTANEOUS) {

            queryEventStatement.setSubscriber(null);
            queryEventStatement.destroy();
        }
    }

}

