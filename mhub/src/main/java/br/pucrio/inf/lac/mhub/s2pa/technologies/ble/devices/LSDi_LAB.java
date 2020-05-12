package br.pucrio.inf.lac.mhub.s2pa.technologies.ble.devices;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.pucrio.inf.lac.mhub.s2pa.base.TechnologyDevice;
import br.pucrio.inf.lac.mhub.s2pa.base.TechnologySensor;

import static java.util.UUID.fromString;

public class LSDi_LAB implements TechnologyDevice {

    private final static UUID UUID_LAB_SERV = fromString("FFE0-0000-1000-8000-00805F9B34FB"); // Service

    private final static UUID UUID_SEN_DATA = fromString("FFE1-0000-1000-8000-00805F9B34FB"); // All Sensors

    private static final byte ENABLE_SENSOR_CODE = 1;

    public enum Sensor implements TechnologySensor {
        IR_SENSOR("Lab Sensors", UUID_LAB_SERV, UUID_SEN_DATA, null) {
            @Override
            public Double[] convert(final byte[] value) {
                return parse(value);
            }
        };

        private static final String MSG_HEADER = "H";
        private static final String MSG_FOOTER = "F";
        private static final int SENSOR_VALUES = 4;

        private String name;
        private final UUID service, data, config;
        private byte enableCode; // See getEnableSensorCode for explanation.
        private String buffer = null;

        /**
         * Constructor called by all the sensors except Gyroscope
         **/
        Sensor(String name, UUID service, UUID data, UUID config) {
            this.name = name;
            this.service = service;
            this.data = data;
            this.config = config;
            this.enableCode = ENABLE_SENSOR_CODE;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public UUID getService() {
            return service;
        }

        @Override
        public UUID getData() {
            return data;
        }

        @Override
        public UUID getConfig() {
            return config;
        }

        @Override
        public UUID getCalibration() {
            return null;
        }

        @Override
        public byte getEnableSensorCode() {
            return enableCode;
        }

        @Override
        public void setCalibrationData(byte[] value) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
        }

        @Override
        public Double[] convert(byte[] value) {
            throw new UnsupportedOperationException("Programmer error, the individual enum classes are supposed to override this method.");
        }

        public Double[] parse(final byte[] value) {
            String str = new String(value);
            if (buffer != null) {
                str = buffer + str;
            }

            String[] token = str.split(";");
            Double[] arr = new Double[SENSOR_VALUES];

            int index = SENSOR_VALUES;
            for (int i = 0; i < token.length; i++) {
                if (token[i].contains(MSG_HEADER)) {
                    index = i;
                    break;
                }
            }

            if (str.contains(MSG_FOOTER)) {
                for (int i = 0; i < SENSOR_VALUES; i++) {
                    arr[i] = Double.parseDouble(token[index + 1]);
                    index++;
                }
                freeBuffer();
                return arr;
            } else {
                addBuffer(str);
                return null;
            }
        }

        private void addBuffer(String str) {
            buffer = str;
        }

        private void freeBuffer() {
            buffer = null;
        }

        public static Sensor getFromServiceName(String serviceName) {
            for (Sensor s : Sensor.values()) {
                if (s.getName().equals(serviceName)) {
                    return s;
                }
            }
            return null;
        }

        public static List<TechnologySensor> getFromServiceUuid(UUID uuid) {
            List<TechnologySensor> temp = new ArrayList<>();
            for (Sensor s : Sensor.values()) {
                if (s.getService().equals(uuid)) {
                    temp.add(s);
                }
            }
            return temp;
        }

        public static Sensor getFromCharacteristicUuid(UUID uuid) {
            for (Sensor s : Sensor.values()) {
                if (s.getData().equals(uuid)) {
                    return s;
                }
            }
            return null;
        }
    }

    @Override
    public TechnologySensor getServiceByName(String serviceName) {
        return Sensor.getFromServiceName(serviceName);
    }

    @Override
    public List<TechnologySensor> getServiceByUUID(UUID uuid) {
        return Sensor.getFromServiceUuid(uuid);
    }

    @Override
    public TechnologySensor getCharacteristicByUUID(UUID uuid) {
        return Sensor.getFromCharacteristicUuid(uuid);
    }

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
