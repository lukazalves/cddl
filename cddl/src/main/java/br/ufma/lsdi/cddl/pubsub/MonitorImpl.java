package br.ufma.lsdi.cddl.pubsub;

import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import br.ufma.lsdi.cddl.listeners.IMonitorListener;
import br.ufma.lsdi.cddl.message.EsperConfig;
import br.ufma.lsdi.cddl.message.Message;
import lombok.val;

/**
 * Created by lcmuniz on 19/02/17.
 */
public final class MonitorImpl implements Monitor {

    private EPServiceProvider epService;
    private EPAdministrator epAdmin;
    private final Map<String, EPStatement> statements = new HashMap<>();

    public MonitorImpl() {
        init();
    }

    private void init() {
        val config = new EsperConfig();
        epService = EPServiceProviderManager.getProvider("prov" + System.currentTimeMillis(), config);
        epAdmin = epService.getEPAdministrator();
    }

    @Override
    public String addRule(String rule, final IMonitorListener monitorListener) {

        //query = "select * from SensorDataMessage where " + query;
        val queryEventStatement = epAdmin.createEPL(rule);
        queryEventStatement.addListener(new UpdateListener() {
            @Override
            public void update(EventBean[] newEvents, EventBean[] oldEvents) {
                val event = newEvents[0];
                val message = (Message) event.getUnderlying();
                monitorListener.onEvent(message);
            }
        });
        val uuid = UUID.randomUUID().toString();
        statements.put(uuid, queryEventStatement);
        return uuid;
    }

    @Override
    public void removeRule(String id) {
        val queryEventStatement = statements.get(id);
        if (queryEventStatement != null) {
            queryEventStatement.removeAllListeners();
        }
        statements.remove(id);
    }

    @Override
    public int getNumRules() {
        return statements.size();
    }

    @Override
    public void messageArrived(final Message message) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                epService.getEPRuntime().sendEvent(message);
            }
        }).start();

    }

}
