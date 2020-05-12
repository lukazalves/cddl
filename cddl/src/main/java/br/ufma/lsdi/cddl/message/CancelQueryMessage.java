package br.ufma.lsdi.cddl.message;

import java.io.Serializable;

/**
 * Created by lcmuniz on 09/03/17.
 */

public final class CancelQueryMessage extends Message implements Serializable {

    private final String returnCode;

    public CancelQueryMessage(String returnCode) {
        this.returnCode = returnCode;
    }

    public String getReturnCode() {
        return returnCode;
    }
}
