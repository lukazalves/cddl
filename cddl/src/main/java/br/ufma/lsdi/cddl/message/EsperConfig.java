package br.ufma.lsdi.cddl.message;

import com.espertech.esper.client.Configuration;

/**
 * Created by lcmuniz on 04/06/17.
 */

public final class EsperConfig extends Configuration {

    public EsperConfig() {

        this.addEventTypeAutoName(Message.class.getPackage().getName());
        this.getEngineDefaults().getThreading().setInsertIntoDispatchPreserveOrder(false);
        this.getEngineDefaults().getThreading().setInsertIntoDispatchTimeout(5000);
        this.getEngineDefaults().getThreading().setListenerDispatchPreserveOrder(false);
        this.getEngineDefaults().getThreading().setListenerDispatchTimeout(5000);
//        this.getEngineDefaults().getViewResources().setShareViews(false);
//        this.getEngineDefaults().getExecution().setThreadingProfile(
//                ConfigurationEngineDefaults.ThreadingProfile.LARGE);
//        this.getEngineDefaults().getEventMeta().
//                setDefaultEventRepresentation(Configuration.EventRepresentation.OBJECTARRAY);
        this.getEngineDefaults().getEventMeta().setClassPropertyResolutionStyle(
                Configuration.PropertyResolutionStyle.CASE_INSENSITIVE);

    }

}
