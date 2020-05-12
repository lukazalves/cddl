package br.ufma.lsdi.cddl.services;

import android.content.Context;

import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import br.ufma.lsdi.cddl.CDDL;
import br.ufma.lsdi.cddl.listeners.ISubscriberListener;
import br.ufma.lsdi.cddl.message.CancelQueryMessage;
import br.ufma.lsdi.cddl.message.EsperConfig;
import br.ufma.lsdi.cddl.message.Message;
import br.ufma.lsdi.cddl.message.QueryMessage;
import br.ufma.lsdi.cddl.message.QueryResponseMessage;
import br.ufma.lsdi.cddl.message.ServiceInformationMessage;
import br.ufma.lsdi.cddl.ontology.QueryType;
import br.ufma.lsdi.cddl.pubsub.Publisher;
import br.ufma.lsdi.cddl.pubsub.PublisherFactory;
import br.ufma.lsdi.cddl.pubsub.Subscriber;
import br.ufma.lsdi.cddl.pubsub.SubscriberFactory;
import br.ufma.lsdi.cddl.util.Asserts;
import br.ufma.lsdi.cddl.util.CDDLEventBus;
import lombok.val;

/**
 * Created by lcmuniz on 05/03/17.
 */
public final class LocalDirectoryImpl implements LocalDirectory {

    private static final String TAG = LocalDirectoryImpl.class.getSimpleName();

    private EPServiceProvider epService;
    private final EPAdministrator epAdmin;

    private final String TIME_WINDOW = "5 sec";

    private final Publisher publisher;
    private final Subscriber subscriber;


    LocalDirectoryImpl(Context context) {

        Asserts.assertNotNull(context, "Context can not be null");

        if (!CDDLEventBus.getDefault().isRegistered(this)) {
            CDDLEventBus.getDefault().register(this);
        }

        val config = new EsperConfig();

        // pega o mesmo provider do QoCEvaluator
        epService = EPServiceProviderManager.getProvider(QoCEvaluatorImpl.TAG);
        epAdmin = epService.getEPAdministrator();

        publisher = PublisherFactory.createPublisher();
        publisher.addConnection(CDDL.getInstance().getConnection());

        subscriber = SubscriberFactory.createSubscriber();
        subscriber.addConnection(CDDL.getInstance().getConnection());
        subscriber.setSubscriberListener(new ISubscriberListener() {
            @Override
            public void onMessageArrived(Message message) {
                if (message instanceof QueryMessage) {
                    processQuery((QueryMessage) message);
                }
                else if (message instanceof CancelQueryMessage) {
                    cancelQuery((CancelQueryMessage) message);
                }
            }
        });
        subscriber.subscribeQueryTopic();
        subscriber.subscribeCancelQueryTopic();

    }

    public void close() {
        if (CDDLEventBus.getDefault().isRegistered(this)) {
            CDDLEventBus.getDefault().unregister(this);
        }

    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(final QueryMessage queryMessage) {
        processQuery(queryMessage);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(final CancelQueryMessage cancelQueryMessage) {
        cancelQuery(cancelQueryMessage);
    }

    private void processQuery(final QueryMessage queryMessage) {

        //String temp = queryMessage.getQuery();
        //final String epl = temp.replace("ServiceInformationMessage", "ServiceInformationMessage.win:time_batch(" + TIME_WINDOW + ")");

        val epl = queryMessage.getQuery();
        final EPStatement queryEventStatement = epAdmin.createEPL(epl, queryMessage.getReturnCode());

//        final ServiceInformationMessageSubscriber subscriber = new ServiceInformationMessageSubscriber(publisher, queryMessage, queryEventStatement);
//        queryEventStatement.setSubscriber(subscriber);

        queryEventStatement.addListener(new UpdateListener() {
            @Override
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                val qrm = new QueryResponseMessage(queryMessage);
                qrm.setSubscriberID(queryMessage.getPublisherID());

                for (EventBean e : newEvents) {
                    val sim = new ServiceInformationMessage();
                    sim.setPublisherID((String) e.get("publisherID"));
                    sim.setServiceName((String) e.get("serviceName"));
                    sim.setAccuracy((Double) e.get("accuracy"));
                    sim.setMeasurementTime(convertToLongIfDouble(e.get("measurementTime")));
                    sim.setAvailableAttributes(convertToIntegerIfDouble(e.get("availableAttributes")));
                    sim.setSourceLocationLatitude((Double) e.get("sourceLocationLatitude"));
                    sim.setSourceLocationLongitude((Double) e.get("sourceLocationLongitude"));
                    sim.setSourceLocationAltitude((Double) e.get("sourceLocationAltitude"));
                    sim.setSourceLocationAltitude((Double) e.get("sourceLocationAccuracy"));
                    sim.setSourceLocationTimestamp(convertToLongIfDouble(e.get("sourceLocationTimestamp")));
                    sim.setMeasurementInterval(convertToLongIfDouble(e.get("measurementInterval")));
                    sim.setNumericalResolution(convertToIntegerIfDouble(e.get("numericalResolution")));
                    qrm.getServiceInformationMessageList().add(sim);
                }
                publisher.publish(qrm);
                //AppUtils.logger('d', TAG, qrm.toString());

                if (queryMessage.getType() == QueryType.INSTANTANEOUS) {
                    queryEventStatement.removeAllListeners();
                    queryEventStatement.destroy();
                }

            }

        });

    }

    private Long convertToLongIfDouble(Object value) {
        if (value instanceof Long) return (Long) value;
        if (value instanceof Double) return ((Double) value).longValue();
        return null;
    }

    private Integer convertToIntegerIfDouble(Object value) {
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Double) return ((Double) value).intValue();
        return null;
    }

    private void cancelQuery(CancelQueryMessage cancelQueryMessage) {
        val queryEventStatement = epAdmin.getStatement(cancelQueryMessage.getReturnCode());
        queryEventStatement.removeAllListeners();
        queryEventStatement.setSubscriber(null);
        queryEventStatement.destroy();
    }
}



