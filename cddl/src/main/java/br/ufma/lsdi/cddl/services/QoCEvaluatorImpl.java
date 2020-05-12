package br.ufma.lsdi.cddl.services;

import android.content.Context;
import android.location.Location;

import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Hashtable;

import br.pucrio.inf.lac.mhub.models.locals.SensorData;
import br.pucrio.inf.lac.mhub.models.locals.SensorDataExtended;
import br.pucrio.inf.lac.mhub.util.MHubEventBus;
import br.ufma.lsdi.cddl.CDDL;
import br.ufma.lsdi.cddl.Connection;
import br.ufma.lsdi.cddl.message.ConnectionChangedStatusMessage;
import br.ufma.lsdi.cddl.message.EsperConfig;
import br.ufma.lsdi.cddl.message.EventQueryMessage;
import br.ufma.lsdi.cddl.message.Message;
import br.ufma.lsdi.cddl.message.ObjectConnectedMessage;
import br.ufma.lsdi.cddl.message.ObjectDisconnectedMessage;
import br.ufma.lsdi.cddl.message.ObjectDiscoveredMessage;
import br.ufma.lsdi.cddl.message.ObjectFoundMessage;
import br.ufma.lsdi.cddl.message.QueryMessage;
import br.ufma.lsdi.cddl.message.QueryResponseMessage;
import br.ufma.lsdi.cddl.message.SensorDataMessage;
import br.ufma.lsdi.cddl.message.ServiceInformationMessage;
import br.ufma.lsdi.cddl.pubsub.CDDLFilter;
import br.ufma.lsdi.cddl.pubsub.CDDLFilterImpl;
import br.ufma.lsdi.cddl.pubsub.Publisher;
import br.ufma.lsdi.cddl.pubsub.PublisherFactory;
import br.ufma.lsdi.cddl.qos.AbstractQoS;
import br.ufma.lsdi.cddl.util.Asserts;
import br.ufma.lsdi.cddl.util.CDDLEventBus;
import br.ufma.lsdi.cddl.util.Time;
import br.ufma.lsdi.cddl.util.Topic;
import lombok.val;

/**
 * Created by lcmuniz on 05/03/17.
 */
public final class  QoCEvaluatorImpl implements QoCEvaluator {

    public static final String TAG = QoCEvaluatorImpl.class.getSimpleName();

    private final EPServiceProvider epService;
    private final EPAdministrator epAdmin;

    private final Publisher publisher;
    private final String TIME_WINDOW = "5 sec";
    private final CDDLFilter defaultCDDLFilter = new CDDLFilterImpl("Select * from Message");
    private final Connection connection;

    private EPStatement epsStatementServiceInformationMessages;
    private EPStatement epsStatementMessages;

    private CDDLFilter cddlFilter = defaultCDDLFilter;
    private Location location;

    private final Hashtable<String, Message> previousMessages = new Hashtable<String, Message>();

    protected QoCEvaluatorImpl(Context context) {

        if (!MHubEventBus.getDefault().isRegistered(this)) {
            MHubEventBus.getDefault().register(this);
        }
        if (!CDDLEventBus.getDefault().isRegistered(this)) {
            CDDLEventBus.getDefault().register(this);
        }

        Asserts.assertNotNull(context, "Context can not be null");

        val config = new EsperConfig();
        epService = EPServiceProviderManager.getProvider(TAG, config);
        epAdmin = epService.getEPAdministrator();

        connection = CDDL.getInstance().getConnection();

        publisher = PublisherFactory.createPublisher();
        publisher.addConnection(connection);


        createStatementForServiceInformationMessages();

        createStatementForSensorDataMessages();

    }

    public void close() {

        if (MHubEventBus.getDefault().isRegistered(this)) {
            MHubEventBus.getDefault().unregister(this);
        }
        if (CDDLEventBus.getDefault().isRegistered(this)) {
            CDDLEventBus.getDefault().unregister(this);
        }

    }



    @Subscribe(threadMode =  ThreadMode.ASYNC)
    public void on(AbstractQoS qos) {
        System.out.println("+++++++++++++"+qos);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(SensorData sensorData) {

        if (sensorData.getAction().equals(SensorData.FOUND)) {
            ObjectFoundMessage o = new ObjectFoundMessage();
            o.setMouuid(sensorData.getMouuid());
            publisher.publish(o);
        }
        else if (sensorData.getAction().equals(SensorData.CONNECTED)) {
            ObjectConnectedMessage o = new ObjectConnectedMessage();
            o.setMouuid(sensorData.getMouuid());
            publisher.publish(o);
        }
        else if (sensorData.getAction().equals(SensorData.DISCONNECTED)) {
            ObjectDisconnectedMessage o = new ObjectDisconnectedMessage();
            o.setMouuid(sensorData.getMouuid());
            publisher.publish(o);
        }
        else if (sensorData.getAction().equals(SensorData.DISCOVERED)) {
            ObjectDiscoveredMessage o = new ObjectDiscoveredMessage();
            o.setMouuid(sensorData.getMouuid());
            o.setServiceList(sensorData.getServiceList());
            publisher.publish(o);
        }
        else if (sensorData.getAction().equals(SensorData.READ)) {

            sendSensorDataMessageToCEP(sensorData);

        }


    }

    private void sendSensorDataMessageToCEP(SensorData sensorData) {

        if (isSensorNameInvalid(sensorData.getSensorName())) return;

        final val message = evaluateQoC(sensorData);

        if (previousMessages.containsKey(message.getServiceName())) {
            val previousMessage = previousMessages.get(message.getServiceName());
            message.setMeasurementInterval(message.getMeasurementTime() - previousMessage.getMeasurementTime());
        }


        if (message.getTopic() == null) {
            val topic = Topic.serviceTopic(connection.getClientId(),message.getServiceName());
            message.setTopic(topic);
        }

        previousMessages.put(message.getServiceName(), message);

        new Thread(new Runnable() {
            @Override
            public void run() {
                epService.getEPRuntime().sendEvent(message);
            }
        }).start();

    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(final Message message) {

        if (isSensorNameInvalid(message.getServiceName())) {
            return;
        }

        if (message == null) {
            return;
        }

        if (message instanceof QueryMessage || message instanceof QueryResponseMessage || message instanceof ServiceInformationMessage || message instanceof EventQueryMessage || message instanceof ConnectionChangedStatusMessage) {
            return;
        }

        evaluateQoC(message);

        if (previousMessages.containsKey(message.getServiceName())) {
            val previousMessage = previousMessages.get(message.getServiceName());
            message.setMeasurementInterval(message.getMeasurementTime() - previousMessage.getMeasurementTime());
        }

        if (message.getTopic() == null) {
            val topic = Topic.serviceTopic(connection.getClientId(), message.getServiceName());
            message.setTopic(topic);
        }

        previousMessages.put(message.getServiceName(), message);

        new Thread(new Runnable() {
            @Override
            public void run() {
                epService.getEPRuntime().sendEvent(message);
            }
        }).start();


    }

    private boolean isSensorNameInvalid(String sensorName) {
        return sensorName == null || sensorName.isEmpty() || sensorName.equalsIgnoreCase("null");
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(CDDLFilterImpl cddlFilter) {
        if (cddlFilter.getEplFilter().equals("")) {
            this.cddlFilter = defaultCDDLFilter;
        } else {
            this.cddlFilter = cddlFilter;
        }
        createStatementForSensorDataMessages();

    }

    @Subscribe(sticky = true, threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(Location location) {
        this.location = location;
    }


    /**
     * calcula parametros de qoc
     *
     * @param sensorData from who qoc must be evaluated
     * @return uma mensagem SensorDataMessage
     */
    private SensorDataMessage evaluateQoC(SensorData sensorData) {

        val sensorDataMessage = new SensorDataMessage(sensorData);

        if (sensorData instanceof SensorDataExtended) {
            val sensorDataExtended = (SensorDataExtended) sensorData;
            sensorDataExtended.setMeasurementTime(Time.getCurrentTimestamp());
            sensorDataMessage.setAccuracy(evaluateQoCAccuracy(sensorDataExtended.getSensorAccuracy()));
            sensorDataMessage.setAvailableAttributes(evaluateQoCAvailableAttributes(sensorDataExtended.getAvailableAttributes(), sensorDataExtended.getSensorValue()));
            sensorDataMessage.setSourceLocationLatitude(evaluateQoCSourceLocationLatitude(sensorDataExtended.getLatitude()));
            sensorDataMessage.setSourceLocationLongitude(evaluateQoCSourceLocationLongitude(sensorDataExtended.getLongitude()));
            sensorDataMessage.setSourceLocationAltitude(evaluateQoCSourceLocationAltitude(sensorDataExtended.getAltitude()));
            sensorDataMessage.setSourceLocationAccuracy(evaluateQoCSourceLocationAccuracy(sensorDataExtended.getLocationAccuracy()));
            sensorDataMessage.setSourceLocationTimestamp(evaluateQoCSourceLocationTimestamp(sensorDataExtended.getLocationTimestamp()));
            sensorDataMessage.setGatewaySpeed(evaluateQoCGatewaySpeed(sensorDataExtended.getSpeed()));
            sensorDataMessage.setNumericalResolution(evaluateQoCNumericalResolution(sensorDataExtended.getNumericalResolution(), sensorDataExtended.getSensorObjectValue(), sensorDataExtended.getSensorValue()));

        } else {
            sensorDataMessage.setMeasurementTime(evaluateQoCMeasurementTime(sensorData.getTimestamp()));
            sensorDataMessage.setSourceLocationLatitude(evaluateQoCSourceLocationLatitude(sensorData.getLatitude()));
            sensorDataMessage.setSourceLocationLongitude(evaluateQoCSourceLocationLongitude(sensorData.getLongitude()));
        }


        sensorDataMessage.setQocEvaluated(true);
        return sensorDataMessage;


    }

    private void evaluateQoC(Message message) {
        message.setAccuracy(evaluateQoCAccuracy(message.getAccuracy()));
        message.setSourceLocationLatitude(evaluateQoCSourceLocationLatitude(message.getSourceLocationLatitude()));
        message.setSourceLocationLongitude(evaluateQoCSourceLocationLongitude(message.getSourceLocationLongitude()));
        message.setSourceLocationAltitude(evaluateQoCSourceLocationAltitude(message.getSourceLocationAltitude()));
        message.setSourceLocationAccuracy(evaluateQoCSourceLocationAccuracy(message.getSourceLocationAccuracy()));
        message.setSourceLocationTimestamp(evaluateQoCSourceLocationTimestamp(message.getSourceLocationTimestamp()));
        message.setGatewaySpeed(evaluateQoCGatewaySpeed(message.getGatewaySpeed()));
        message.setNumericalResolution(evaluateQoCNumericalResolution(message.getNumericalResolution(), message.getServiceByteArray()));
        message.setQocEvaluated(true);
    }

    private Integer evaluateQoCNumericalResolution(Integer numericalResolution, Object sensorObjectValue, Double[] sensorValue) {

        if (numericalResolution != null) return numericalResolution;

        if (sensorObjectValue != null) {

            BigDecimal big = BigDecimal.ZERO;

            if (sensorObjectValue instanceof Double) {
                val valor = (Double) sensorObjectValue;
                big = BigDecimal.valueOf(valor);
            } else if (sensorObjectValue instanceof Double[]) {
                val valor = (Double[]) sensorObjectValue;
                big = BigDecimal.valueOf(valor[0]);
            } else if (sensorObjectValue instanceof double[]) {
                val valor = (double[]) sensorObjectValue;
                big = BigDecimal.valueOf(valor[0]);
            } else if (sensorObjectValue instanceof Float) {
                val valor = (Float) sensorObjectValue;
                big = BigDecimal.valueOf(valor);
            } else if (sensorObjectValue instanceof Float[]) {
                val valor = (Float[]) sensorObjectValue;
                big = BigDecimal.valueOf(valor[0]);
            } else if (sensorObjectValue instanceof float[]) {
                val valor = (float[]) sensorObjectValue;
                big = BigDecimal.valueOf(valor[0]);
            }
            return getNumberOfDecimalPlaces(big);

        } else if (sensorValue != null) {
            return getNumberOfDecimalPlaces(BigDecimal.valueOf(sensorValue[0]));
        }

        return 0;

    }

    private Integer evaluateQoCNumericalResolution(Integer numericalResolution, Object sensorObjectValue) {

        if (numericalResolution != null) return numericalResolution;

        if (sensorObjectValue != null) {

            BigDecimal big = BigDecimal.ZERO;
            if (sensorObjectValue instanceof Double) {
                val valor = (Double) sensorObjectValue;
                big = BigDecimal.valueOf(valor);
            } else if (sensorObjectValue instanceof Double[]) {
                val valor = (Double[]) sensorObjectValue;
                big = BigDecimal.valueOf(valor[0]);
            } else if (sensorObjectValue instanceof double[]) {
                val valor = (double[]) sensorObjectValue;
                big = BigDecimal.valueOf(valor[0]);
            } else if (sensorObjectValue instanceof Float) {
                val valor = (Float) sensorObjectValue;
                big = BigDecimal.valueOf(valor);
            } else if (sensorObjectValue instanceof Float[]) {
                val valor = (Float[]) sensorObjectValue;
                big = BigDecimal.valueOf(valor[0]);
            } else if (sensorObjectValue instanceof float[]) {
                val valor = (float[]) sensorObjectValue;
                big = BigDecimal.valueOf(valor[0]);
            }
            return getNumberOfDecimalPlaces(big);

        }

        return 0;

    }

    private Integer getNumberOfDecimalPlaces(BigDecimal bigDecimal) {
        if (bigDecimal == null) return 0;
        return Math.max(0, bigDecimal.stripTrailingZeros().scale());
    }

    private Double evaluateQoCSourceLocationLatitude(Double latitude) {
        if (latitude == null) {
            if (location == null) {
                return null;
            } else {
                return location.getLatitude();
            }
        } else {
            return latitude;
        }
    }

    private Double evaluateQoCSourceLocationAltitude(Double altitude) {
        if (altitude == null) {
            if (location == null) {
                return null;
            } else {
                return location.getAltitude();
            }
        } else {
            return altitude;
        }
    }


    private Double evaluateQoCSourceLocationLongitude(Double longitude) {
        if (longitude == null) {
            if (location == null) {
                return null;
            } else {
                return location.getLongitude();
            }
        } else {
            return longitude;
        }
    }


    private Double evaluateQoCSourceLocationAccuracy(Double accuracy) {
        if (accuracy == null) {
            if (location == null) {
                return null;
            }
            return (double) location.getAccuracy();
        }
        return accuracy;
    }


    private Double evaluateQoCGatewaySpeed(Double gatewaySpeed) {
        if (gatewaySpeed == null) {
            if (location == null) {
                return null;
            }
            return (double) location.getSpeed();
        }
        return gatewaySpeed;
    }

    private Long evaluateQoCSourceLocationTimestamp(Long timestamp) {
        if (timestamp == null) {
            if (location == null) {
                return null;
            }
            return location.getTime();
        }
        return timestamp;
    }

    private Integer evaluateQoCAvailableAttributes(Integer availableAttributes, Double[] values) {

        if (availableAttributes != null && availableAttributes > 0) {
            return availableAttributes;
        }

        if (values != null) {
            return values.length;
        }

        return 1;

    }

    private Integer evaluateQoCAvailableAttributes(Integer availableAttributes, double[] values) {

        if (availableAttributes != null) {
            return availableAttributes;
        }

        if (values != null) {
            return values.length;
        }

        return 1;

    }

    private Integer evaluateQoCAvailableAttributes(Integer availableAttributes, Float[] values) {

        if (availableAttributes != null) {
            return availableAttributes;
        }

        if (values != null) {
            return values.length;
        }

        return 1;

    }

    private Integer evaluateQoCAvailableAttributes(Integer availableAttributes, float[] values) {

        if (availableAttributes != null) {
            return availableAttributes;
        }

        if (values != null) {
            return values.length;
        }

        return 1;

    }

    private Integer evaluateQoCAvailableAttributes(Integer availableAttributes, Integer[] values) {

        if (availableAttributes != null) {
            return availableAttributes;
        }

        if (values != null) {
            return values.length;
        }

        return 1;

    }

    private Integer evaluateQoCAvailableAttributes(Integer availableAttributes, int[] values) {

        if (availableAttributes != null) {
            return availableAttributes;
        }

        if (values != null) {
            return values.length;
        }

        return 1;

    }

    private Integer evaluateQoCAvailableAttributes(Integer availableAttributes, Long[] values) {

        if (availableAttributes != null) {
            return availableAttributes;
        }

        if (values != null) {
            return values.length;
        }

        return 1;

    }

    private Integer evaluateQoCAvailableAttributes(Integer availableAttributes, BigDecimal[] values) {

        if (availableAttributes != null) {
            return availableAttributes;
        }

        if (values != null) {
            return values.length;
        }

        return 1;

    }

    private Integer evaluateQoCAvailableAttributes(Integer availableAttributes, BigInteger[] values) {

        if (availableAttributes != null) {
            return availableAttributes;
        }

        if (values != null) {
            return values.length;
        }

        return 1;

    }

    private Integer evaluateQoCAvailableAttributes(Integer availableAttributes, Number[] values) {

        if (availableAttributes != null) {
            return availableAttributes;
        }

        if (values != null) {
            return values.length;
        }

        return 1;

    }

    private Integer evaluateQoCAvailableAttributes(Integer availableAttributes, String[] values) {

        if (availableAttributes != null) {
            return availableAttributes;
        }

        if (values != null) {
            return values.length;
        }

        return 1;

    }

    private Integer evaluateQoCAvailableAttributes(Integer availableAttributes, Object[] values) {

        if (availableAttributes != null) {
            return availableAttributes;
        }

        if (values != null) {
            return values.length;
        }

        return 1;

    }

    private Double evaluateQoCAccuracy(Double accuracy) {
        return accuracy;
    }

    private Long evaluateQoCMeasurementTime(Long measurementTime) {
        if (measurementTime == null) {
            return Time.getCurrentTimestamp();
        }

        if (measurementTime <= 0) {
            return Time.getCurrentTimestamp();
        }

        return measurementTime;
    }


    /**
     * calcula a media das qoc e envia para o fluxo de eventos de ServiceInformationMessage
     */
    private void createStatementForServiceInformationMessages() {
        val epl = "insert into ServiceInformationMessage(publisherID, serviceName, accuracy, measurementTime, availableAttributes, sourceLocationLatitude, sourceLocationLongitude, sourceLocationAltitude, sourceLocationAccuracy, sourceLocationTimestamp, gatewaySpeed, measurementInterval, numericalResolution) select publisherID, serviceName, avg(accuracy), avg(measurementTime), avg(availableAttributes), avg(sourceLocationLatitude), avg(sourceLocationLongitude), avg(sourceLocationAltitude), avg(sourceLocationAccuracy), avg(sourceLocationTimestamp),avg(gatewaySpeed), avg(measurementInterval), avg(numericalResolution) from Message.win:time(" + TIME_WINDOW + ") group by publisherID, serviceName";
        epsStatementServiceInformationMessages = epAdmin.createEPL(epl);
        epsStatementServiceInformationMessages.start();
    }

    private void createStatementForSensorDataMessages() {

        if (epsStatementMessages != null) {
            epsStatementMessages.removeAllListeners();
            epsStatementMessages.destroy();
        }

        epsStatementMessages = epAdmin.createEPL(cddlFilter.getEplFilter());

        epsStatementMessages.addListener(new UpdateListener() {
            @Override
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                if (newEvents != null) {
                    for (final EventBean e : newEvents) {
                        val message = (Message) e.getUnderlying();
                        if (message.publisher != null) {
                            message.publisher.publish(message);
                        }
                        else {
                            publisher.publish(message);
                        }
                    }
                }
            }

        });

    }

}
