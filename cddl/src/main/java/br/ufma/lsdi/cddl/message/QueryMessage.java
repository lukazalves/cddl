package br.ufma.lsdi.cddl.message;

import java.io.Serializable;

import br.ufma.lsdi.cddl.ontology.QueryType;

/**
 * A service query message that the subscriber sends to the directory asking for
 * informations about publishers. The message is composed by the subscriber ID
 * and a query. The query is created on a specific language called Information
 * Context Query Language (ICCL). A query example: service.location with
 * qoc.precision = 98 The example above, asks the directory for all publisher
 * that can publishMessage location context information with a precision of 98%. See
 * the iccl documentation for more information.
 *
 * @author lcmuniz
 * @since June 26, 2016
 */
public final class QueryMessage extends Message implements Serializable {

    private static final long serialVersionUID = 7658311196412694942L;

    private final String timestamp; // message created time
    private final String query;
    private final QueryType type;
    private final String returnCode;

    public QueryMessage(String query, QueryType type, String returnCode) {
        this.timestamp = Long.valueOf(System.currentTimeMillis()).toString();
        this.query = query;
        this.type = type;
        this.returnCode = returnCode;
        setQocEvaluated(true);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getQuery() {
        return query;
    }

    public QueryType getType() {
        return type;
    }

    public String getReturnCode() {
        return returnCode;
    }

}
