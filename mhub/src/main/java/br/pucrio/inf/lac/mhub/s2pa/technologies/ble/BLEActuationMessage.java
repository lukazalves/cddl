package br.pucrio.inf.lac.mhub.s2pa.technologies.ble;

/**
 * Created by sheri on 21/01/2018.
 */

public class BLEActuationMessage {
    private String characteristicUUID;
    private byte[] command;

    public String getCharacteristicUUID() {
        return characteristicUUID;
    }
    public byte[] getCommand() {
        return command;
    }

    public BLEActuationMessage(String device, String serviceUUID, String characteristicUUID, byte[] command) {
        this.characteristicUUID = characteristicUUID;
        this.command = command;
    }
}
