package br.pucrio.inf.lac.mhub.s2pa.technologies.bt.sensors;


import java.util.UUID;

import br.pucrio.inf.lac.mhub.models.locals.SensorData;
import br.pucrio.inf.lac.mhub.models.locals.SensorDataExtended;

public class HXM030655_CadenceSensor extends HXM030655_AbstractSensor {

    private static HXM030655_CadenceSensor instance;

    private HXM030655_CadenceSensor() {
        super();
    }

    public static HXM030655_CadenceSensor getInstance() {
        if (instance == null) {
            instance = new HXM030655_CadenceSensor();
        }
        return instance;
    }


    @Override
    public UUID getCalibration() {
        return null;
    }

    @Override
    public void setCalibrationData(byte[] value) throws UnsupportedOperationException {

    }

    @Override
    public synchronized Double[] convert(byte[] bytes) {
        // TODO Auto-generated method stub
        double cadence = getHRSpeedDistPacketInfo().GetCadence(bytes);
        return new Double[]{cadence};
    }

    public SensorData convertToSensorData(byte[] bytes) throws UnsupportedOperationException {
        Double[] values = convert(bytes);
        SensorDataExtended sensorDataExtended = new SensorDataExtended();
        sensorDataExtended.setSensorName(getName());
        sensorDataExtended.setSensorValue(values);
        sensorDataExtended.setSensorObjectValue(values);
        return sensorDataExtended;
    }

    @Override
    public int getPacketMsgID() {
        // TODO Auto-generated method stub
        return HR_SPD_DIST_PACKET;
    }


}
