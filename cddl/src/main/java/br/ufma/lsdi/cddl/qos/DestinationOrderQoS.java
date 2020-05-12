package br.ufma.lsdi.cddl.qos;

import java.util.Comparator;

import br.ufma.lsdi.cddl.message.Message;

public final class DestinationOrderQoS extends AbstractQoS implements
        Comparator<Message> {

    private int kind = DEFAULT_KIND;

    public static final int MENSUREMENT_TIMESTAMP_KIND = 1;

    public static final int PUBLISHER_TIMESTAMP = 2;

    public static final int SUBSCRIBER_TIMESTAMP = 3;

    public static final int DEFAULT_KIND = SUBSCRIBER_TIMESTAMP;

    public int getKind() {
        return kind;
    }

    public void setKind(int kind) throws IllegalArgumentException {

        if (kind > SUBSCRIBER_TIMESTAMP || kind < MENSUREMENT_TIMESTAMP_KIND) {
            throw new IllegalArgumentException(
                    "O valor não pode ser maior que " + Long.MAX_VALUE
                            + " e nem menor que " + MENSUREMENT_TIMESTAMP_KIND);
        }

        this.kind = kind;
    }

    @Override
    public void restoreDefaultQoS() {
        this.kind = DEFAULT_KIND;
    }

    @Override
    public int compare(Message lhs, Message rhs) {

        if (kind == SUBSCRIBER_TIMESTAMP && lhs.getReceptionTimestamp() != 0
                && rhs.getReceptionTimestamp() != 0) {

            if (lhs.getReceptionTimestamp() > rhs.getReceptionTimestamp()) {
                return -1;
            }
            if (lhs.getReceptionTimestamp() < rhs.getReceptionTimestamp()) {
                return 1;
            }

            return 0;

        } else if (kind == PUBLISHER_TIMESTAMP
                && lhs.getPublicationTimestamp() != 0
                && rhs.getPublicationTimestamp() != 0) {

            if (lhs.getPublicationTimestamp() > rhs.getPublicationTimestamp()) {
                return 1;
            }
            if (lhs.getPublicationTimestamp() < rhs.getPublicationTimestamp()) {
                return -1;
            }

            return 0;

        } else if (kind == MENSUREMENT_TIMESTAMP_KIND
                && lhs.getMeasurementTime() != 0
                && rhs.getMeasurementTime() != 0) {

            if (lhs.getMeasurementTime() > rhs.getMeasurementTime()) {
                return 1;
            }
            if (lhs.getMeasurementTime() < rhs.getMeasurementTime()) {
                return -1;
            }

            return 0;

        }

        return 0;
    }

}
