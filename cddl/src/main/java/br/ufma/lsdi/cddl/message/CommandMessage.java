package br.ufma.lsdi.cddl.message;

import java.io.Serializable;
import java.util.UUID;

import lombok.Getter;

/**
 * @author lcmuniz
 * @since June 26, 2016
 */
@Getter
public final class CommandMessage extends Message implements Serializable {

    private static final long serialVersionUID = 7658311196412694942L;

    private String to;
    private String uuid;
    private String serviceName;
    private Object commandValue;

    public CommandMessage(String to, String mouuid, String serviceName, Object commandValue) {
        this.to = to;
        this.uuid = UUID.randomUUID().toString();
        this.setMouuid(mouuid);
        this.serviceName = serviceName;
        this.commandValue = commandValue;
    }

    @Override
    public String toString() {
        return to + getMouuid() + serviceName + commandValue;
    }
}
