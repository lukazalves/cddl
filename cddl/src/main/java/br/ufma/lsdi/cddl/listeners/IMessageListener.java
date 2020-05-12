package br.ufma.lsdi.cddl.listeners;

import br.ufma.lsdi.cddl.message.Message;

public interface IMessageListener {

    void onMessageArrived(Message message);

}
