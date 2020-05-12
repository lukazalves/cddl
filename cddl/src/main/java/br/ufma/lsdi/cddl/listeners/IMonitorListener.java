package br.ufma.lsdi.cddl.listeners;

import br.ufma.lsdi.cddl.message.Message;

/**
 * Created by lcmuniz on 19/02/17.
 */
public interface IMonitorListener {

    void onEvent(Message message);

}
