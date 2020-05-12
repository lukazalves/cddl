package br.ufma.lsdi.cddl.util;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

/**
 * Created by bertodetacio on 10/06/17.
 */

public class CDDLEventBus extends EventBus {

    private static CDDLEventBus instance;

    public static CDDLEventBus getDefault() {
        if (instance == null) {
            instance = new CDDLEventBus();
        }
        return instance;
    }


}
