/**
 *
 */
package br.ufma.lsdi.cddl.message;

import java.io.Serializable;

/**
 * @author bertodetacio
 */
public class LivelinessMessage extends Message implements Serializable {

    public LivelinessMessage() {
        super();
        setQocEvaluated(true);
    }

}
