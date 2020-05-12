package br.ufma.lsdi.cddl.pubsub;

import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import br.ufma.lsdi.cddl.message.EsperConfig;
import br.ufma.lsdi.cddl.message.Message;
import lombok.val;

/**
 * Created by lcmuniz on 19/02/17.
 */
public final class FilterImpl implements Filter {

    private final String providerName = "prov" + System.currentTimeMillis();
    private EPServiceProvider epService;
    private EPAdministrator epAdmin;

    private final ClientImpl callback;

    public FilterImpl(ClientImpl callback) {
        this.callback = callback;
        init();
    }

    @Override
    public boolean isSet() {
        return epAdmin.getStatementNames().length != 0;
    }

    private void init() {

        val config = new EsperConfig();
        epService = EPServiceProviderManager.getProvider(providerName, config);
        epAdmin = epService.getEPAdministrator();

    }

    @Override
    public void set(String query) {
        clear();
        val queryEventStatement = epAdmin.createEPL(query);
        queryEventStatement.addListener(new UpdateListener() {
            @Override
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                val event = newEvents[0];
                val message = (Message) event.getUnderlying();
                callback.send(message, message.getTopic());
            }
        });
    }

    @Override
    public void clear() {
        epAdmin.destroyAllStatements();
    }

    @Override
    public void process(final Message message) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                epService.getEPRuntime().sendEvent(message);
            }
        }).start();

    }

}
