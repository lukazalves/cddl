package br.ufma.lsdi.cddl.listeners;

import br.ufma.lsdi.cddl.message.Message;

/**
 * Created by bertodetacio on 11/01/17.
 */

public interface IClientQoSListener {

    void onExpectedDeadlineMissed();

    void onExpectedDeadlineFulfilled();

    void onExpectedLivelinessMissed();

    void onExpectedLivelinessFulfilled();

    void onLifespanExpired(Message message);

    void onClientConnectionChangedStatus(String clientId, int status);

}
