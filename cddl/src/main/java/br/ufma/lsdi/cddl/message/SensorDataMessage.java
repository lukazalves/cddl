package br.ufma.lsdi.cddl.message;


import br.pucrio.inf.lac.mhub.models.locals.SensorData;
import br.pucrio.inf.lac.mhub.models.locals.SensorDataExtended;
import lombok.val;

public class SensorDataMessage extends Message {

    public SensorDataMessage() {
        super();
    }

    public SensorDataMessage(SensorData sensorData) {

        this();

        setServiceName(sensorData.getSensorName());
        setMeasurementTime(sensorData.getTimestamp());
        setServiceByteArray(sensorData.getSensorValue());
        setMeasurementTime(sensorData.getTimestamp());
        setMouuid(sensorData.getMouuid());
        setServiceList(sensorData.getServiceList());


        // armazena valor do objeto de acordo com o tipo de dado
        if (sensorData instanceof SensorDataExtended) {
            val sensorDataExtended = (SensorDataExtended) sensorData;
            if (sensorDataExtended.getSensorObjectValue() != null) {
                setServiceByteArray(sensorDataExtended.getSensorObjectValue());
                setMeasurementTime(sensorDataExtended.getMeasurementTime());
                setAvailableAttributes(sensorDataExtended.getAvailableAttributes());
                setAvailableAttributesList(sensorDataExtended.getAvailableAttributesList());
            }
        }

    }

}
