package br.ufma.lsdi.cddl.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import br.ufma.lsdi.cddl.listeners.IPublisherListener;
import br.ufma.lsdi.cddl.pubsub.Publisher;
import br.ufma.lsdi.cddl.util.Time;
import lombok.val;

public class Message extends MqttMessage implements Serializable {

    public transient Publisher publisher;

    public static final String TAG = Message.class.getName();

    private final String className;

    private String uuid = UUID.randomUUID().toString();

    private Long expirationTime;

    private Long publicationTimestamp;

    private String publisherID;

    private Long receptionTimestamp;

    private String topic;

    private String jason;

    private IPublisherListener publisherListener;

    private String serviceName;

    private byte[] serviceByteArray;

    private Object[] serviceValue;

    private Double accuracy;

    private Integer numericalResolution;

    private Long measurementInterval;

    private Integer availableAttributes;

    private String[] availableAttributesList;

    private Long measurementTime = Time.getCurrentTimestamp();

    private List<String> serviceList;

    private String mouuid;

    private Long age;

    private Double sourceLocationLatitude;

    private Double sourceLocationLongitude;

    private Double sourceLocationAltitude;

    private Double sourceLocationAccuracy;

    private Double gatewaySpeed;

    private Long sourceLocationTimestamp;

    private boolean delivered = false;

    private boolean deliveredFailed = false;

    private boolean qocEvaluated = false;

    public Message() {
        className = getClass().getName();
    }

    public static Message convertFromPayload(byte[] payload) {
        return new Gson().fromJson(new String(payload), Message.class);
    }

    public static Message convertFromPayload(byte[] payload, Class<?> clazz) {
        return (Message) new Gson().fromJson(new String(payload), clazz);
    }

    public static Message convertFromObject(Object object, Class<?> clazz) {
        return (Message) new Gson().fromJson(object.toString(), clazz);
    }

    // getters and setters

    public String getClassName() {
        return className;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public Long getPublicationTimestamp() {
        return publicationTimestamp;
    }

    public void setPublicationTimestamp(Long publicationTimestamp) {
        this.publicationTimestamp = publicationTimestamp;
    }

    public String getPublisherID() {
        return publisherID;
    }

    public void setPublisherID(String publisherID) {
        this.publisherID = publisherID;
    }

    public Long getReceptionTimestamp() {
        return receptionTimestamp;
    }

    public void setReceptionTimestamp(Long receptionTimestamp) {
        this.receptionTimestamp = receptionTimestamp;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getUuid() {
        return uuid;
    }

    // tostring, equals and hashcode

    public String toJson() {
        if (jason == null) {
            val gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            jason = gson.toJson(this);
        }
        return jason;
    }

    public String toString() {
        return toJson();
    }

    public IPublisherListener getPublisherListener() {
        return publisherListener;
    }

    public void setPublisherListener(IPublisherListener publisherListener) {
        this.publisherListener = publisherListener;
    }

    public Double getSourceLocationLatitude() {
        return sourceLocationLatitude;
    }

    public void setSourceLocationLatitude(Double sourceLocationLatitude) {
        this.sourceLocationLatitude = sourceLocationLatitude;
    }

    public Double getSourceLocationLongitude() {
        return sourceLocationLongitude;
    }

    public void setSourceLocationLongitude(Double sourceLocationLongitude) {
        this.sourceLocationLongitude = sourceLocationLongitude;
    }

    public Double getSourceLocationAltitude() {
        return sourceLocationAltitude;
    }

    public void setSourceLocationAltitude(Double sourceLocationAltitude) {
        this.sourceLocationAltitude = sourceLocationAltitude;
    }

    public Double getSourceLocationAccuracy() {
        return sourceLocationAccuracy;
    }

    public void setSourceLocationAccuracy(Double sourceLocationAccuracy) {
        this.sourceLocationAccuracy = sourceLocationAccuracy;
    }

    public Long getSourceLocationTimestamp() {
        return sourceLocationTimestamp;
    }

    public void setSourceLocationTimestamp(Long sourceLocationTimestamp) {
        this.sourceLocationTimestamp = sourceLocationTimestamp;
    }

    public long getDelay() {
        return getReceptionTimestamp() - getPublicationTimestamp();
    }


    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public boolean isDeliveredFailed() {
        return deliveredFailed;
    }

    public void setDeliveredFailed(boolean deliveredFailed) {
        this.deliveredFailed = deliveredFailed;
    }

    @Override
    public boolean equals(Object other) {

        if (other == null) {
            return false;
        } else if (other instanceof Message) {

            val message = (Message) other;

            return this.uuid.equalsIgnoreCase(message.getUuid());

        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            return uuid.hashCode();
        } else {
            return super.hashCode();
        }
    }

    public int getReliability() {
        return getQos();
    }

    public void setReliability(int reliability) {
        setQos(reliability);
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Object getServiceByteArray() {
        if (serviceByteArray == null) return null;
        return SerializationUtils.deserialize(serviceByteArray);
    }

    public void setServiceByteArray(Serializable serviceByteArray) {
        if (serviceByteArray instanceof Object[]) {
            this.serviceValue = (Object[]) serviceByteArray;
        }
        else {
            this.serviceValue= new Object[1];
            this.serviceValue[0] = serviceByteArray;
        }
        this.serviceByteArray = SerializationUtils.serialize(serviceByteArray);

    }

    public void setServiceValue(Serializable value) {
        this.setServiceByteArray(value);
    }

    public Object[] getServiceValue() {
        return serviceValue;
    }

    public Integer getNumericalResolution() {
        return numericalResolution;
    }

    public void setNumericalResolution(Integer numericalResolution) {
        this.numericalResolution = numericalResolution;
    }

    public Long getMeasurementInterval() {
        return measurementInterval;
    }

    public void setMeasurementInterval(Long measurementInterval) {
        this.measurementInterval = measurementInterval;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Integer getAvailableAttributes() {
        if (getAvailableAttributesList() == null || getAvailableAttributesList().length == 0) {
            return availableAttributes;
        }
        return getAvailableAttributesList().length;
    }

    public void setAvailableAttributes(Integer availableAttributes) {
        this.availableAttributes = availableAttributes;
    }

    public Long getMeasurementTime() {
        return measurementTime;
    }

    public void setMeasurementTime(Long measurementTime) {
        this.measurementTime = measurementTime;
    }

    public String getMouuid() {
        return mouuid;
    }

    public void setMouuid(String mouuid) {
        this.mouuid = mouuid;
    }

    public List<String> getServiceList() {
        return serviceList;
    }

    public void setServiceList(List<String> serviceList) {
        this.serviceList = serviceList;
    }
    public Long getAge() {
        return Time.getCurrentTimestamp() - measurementTime;
    }

    public Long getTotalDeliveryTime() {
        return getReceptionTimestamp() - measurementTime;
    }

    public boolean isQocEvaluated() {
        return qocEvaluated;
    }

    public void setQocEvaluated(boolean qocEvaluated) {
        this.qocEvaluated = qocEvaluated;
    }

    public String[] getAvailableAttributesList() {
        if (availableAttributesList == null) {
            availableAttributesList = new String[]{serviceName};
        }
        return availableAttributesList;
    }

    public void setAvailableAttributesList(String[] availableAttributesList) {
        this.availableAttributesList = availableAttributesList;
    }

    public Double getGatewaySpeed() {
        return gatewaySpeed;
    }

    public void setGatewaySpeed(Double gatewaySpeed) {
        this.gatewaySpeed = gatewaySpeed;
    }

}
