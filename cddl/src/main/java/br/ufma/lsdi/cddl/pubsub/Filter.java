package br.ufma.lsdi.cddl.pubsub;

import br.ufma.lsdi.cddl.message.Message;

/**
 * Created by lcmuniz on 19/02/17.
 */
public interface Filter {

    void process(Message message);

    boolean isSet();

    void set(String eplFilter);

    void clear();

}
