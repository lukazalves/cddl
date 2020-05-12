package br.ufma.lsdi.cddl.pubsub;

import br.ufma.lsdi.cddl.listeners.IMonitorListener;
import br.ufma.lsdi.cddl.message.Message;

/**
 * Created by lcmuniz on 19/02/17.
 */
public interface Monitor {

    int getNumRules();

    void messageArrived(Message message);

    String addRule(String rule, final IMonitorListener monitorListener);

    void removeRule(String id);

}
