/**
 *
 */
package br.ufma.lsdi.cddl.message;

import java.io.Serializable;

/**
 * @author bertodetacio
 */
public final class ConnectionChangedStatusMessage extends LivelinessMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private int status = CLIENT_CONNECTED;

    private String brokerAddress;

    private String connectionId;

    public static final int CLIENT_CONNECTED = 1;

    public static final int CLIENT_SELF_DESCONNECTED = 2;

    public static final int CLIENT_DESCONNECTED_BY_FAILURE = 3;

    public ConnectionChangedStatusMessage() {
        super();
        setQocEvaluated(true);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {

        if (status > CLIENT_DESCONNECTED_BY_FAILURE || status < CLIENT_CONNECTED) {
            throw new IllegalArgumentException("O valor não pode ser maior que " + CLIENT_SELF_DESCONNECTED + " e nem menor que " + CLIENT_CONNECTED);
        }

        this.status = status;
    }

    public String getBrokerAddress() {
        return brokerAddress;
    }

    public void setBrokerAddress(String brokerAddress) {
        this.brokerAddress = brokerAddress;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }
}
