/**
 *
 */
package br.ufma.lsdi.cddl.message;

import java.io.Serializable;
import java.util.List;

/**
 * @author bertodetacio
 */
public final class DiscoveredServicesMessage extends Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private long technologyID;

    private String macAddress;

    private List<String> servicesList;

    public DiscoveredServicesMessage() {
        super();
    }

    public long getTechnologyID() {
        return technologyID;
    }

    public void setTechnologyID(long technologyID) {
        this.technologyID = technologyID;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public List<String> getServicesList() {
        return servicesList;
    }

    public void setServicesList(List<String> servicesList) {
        this.servicesList = servicesList;
    }

}
