package br.ufma.lsdi.cddl.listeners;

import br.ufma.lsdi.cddl.message.Message;

/**
 * Created by lcmuniz on 26/02/17.
 */

public interface IPublisherListener extends IClientListener {

    void onMessageDelivered(Message message);

}
