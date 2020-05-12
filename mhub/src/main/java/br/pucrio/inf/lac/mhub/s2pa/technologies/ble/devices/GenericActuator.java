package br.pucrio.inf.lac.mhub.s2pa.technologies.ble.devices;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.pucrio.inf.lac.mhub.s2pa.base.TechnologyDevice;
import br.pucrio.inf.lac.mhub.s2pa.base.TechnologySensor;

/**
 * Created by sheri on 07/01/2018.
 */

public class GenericActuator implements TechnologyDevice {

    private class Sensor implements TechnologySensor {
        private String name;
//        private final UUID service, data, config, calibration;
//        private byte enableCode; // See getEnableSensorCode for explanation.
//        public int[] coefficients; // Calibration coefficients

        /**
         * Constructor called by the Gyroscope because he needs a different enabler code.
         **/
        Sensor( String name) {
            this.name    = name;
//            this.service = service;
//            this.data    = data;
//            this.config  = config;
//            this.enableCode  = enableCode;
//            this.calibration = calibration;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public UUID getService() {
            return null;
        }

        @Override
        public UUID getData() {
            return null;
        }

        @Override
        public UUID getConfig() {
            return null;
        }

        @Override
        public UUID getCalibration() {
            return null;
        }

        @Override
        public byte getEnableSensorCode() {
            return 0;
        }

        @Override
        public void setCalibrationData( byte[] value ) throws UnsupportedOperationException {
            throw new UnsupportedOperationException( "Programmer error, the individual enum classes are supposed to override this method." );
        }

        @Override
        public Double[] convert( byte[] value ) throws UnsupportedOperationException {
            throw new UnsupportedOperationException( "Programmer error, the individual enum classes are supposed to override this method." );
        }
    }

    private Sensor sensor;
    public GenericActuator(String deviceName) {
        this.sensor = new Sensor(deviceName);
    }

    @Override
    public TechnologySensor getServiceByName(String serviceName) {
        return this.sensor;
    }

    @Override
    public List<TechnologySensor> getServiceByUUID(UUID uuid) {
        List<TechnologySensor> temp = new ArrayList<>();
        temp.add(this.sensor);
        return temp;
    }

    @Override
    public TechnologySensor getCharacteristicByUUID(UUID uuid) { return this.sensor; }

    @Override
    public boolean initialize(Object o) {
        return false;
    }

    @Override
    public boolean loadState(Object o) {
        return false;
    }

    @Override
    public Object getState() {
        return null;
    }

    @Override
    public String getVersion() {
        return "0.1";
    }
}
