package br.ufma.lsdi.cddl.message;

import java.io.Serializable;
import java.util.function.Predicate;

public final class EventQueryMessage extends Message implements Serializable {

    private static final long serialVersionUID = 7658311196412694942L;
    private final long returnCode;
    private final String timestamp; // message created time
    private Predicate<SensorDataMessage> predicate;
    private String sql;

    public EventQueryMessage(String publisherId, String sql, long returnCode) {
        this.timestamp = Long.valueOf(System.currentTimeMillis()).toString();
        this.setPublisherID(publisherId);
        this.sql = sql;
        this.returnCode = returnCode;
        setQocEvaluated(true);
    }

    public EventQueryMessage(String publisherId, Predicate<SensorDataMessage> predicate, long returnCode) {
        this.timestamp = Long.valueOf(System.currentTimeMillis()).toString();
        this.setPublisherID(publisherId);
        this.predicate = predicate;
        this.returnCode = returnCode;
        setQocEvaluated(true);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSQL() {
        return sql;
    }

    public Predicate<SensorDataMessage> getPredicate() {
        return predicate;
    }

    public long getReturnCode() {
        return returnCode;
    }

}
