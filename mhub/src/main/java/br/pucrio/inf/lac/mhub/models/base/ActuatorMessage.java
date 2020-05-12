package br.pucrio.inf.lac.mhub.models.base;

import java.util.UUID;

import br.pucrio.inf.lac.mhub.components.MOUUID;

public class ActuatorMessage {

    private final String to;
    private final String uuid;
    private final MOUUID mouuid;
    private final String serviceName;
    private final Object serviceValue;

    public ActuatorMessage(String to, MOUUID mouuid, String serviceName, Object serviceValue) {
        this.to = to;
        this.uuid = UUID.randomUUID().toString();
        this.mouuid = mouuid;
        this.serviceName = serviceName;
        this.serviceValue = serviceValue;
    }

    public MOUUID getMouuid() {
        return mouuid;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Object getServiceValue() {
        return serviceValue;
    }
}
