package br.ufma.lsdi.cddl.util;

import java.util.Date;

/**
 * @author bertodetacio
 */
public final class Time {

    private Time() {}

    public static synchronized long getCurrentTimestamp() {
        return new Date().getTime();
    }

    public static Date getData() {
        return new Date();
    }

}
