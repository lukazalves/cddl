package br.ufma.lsdi.cddl.qos;

public final class LifespanQoS extends AbstractQoS {

    private long expirationTime = INFINITE_DURATION;

    private int timestampKind = DEFAULT_TIMESTAMP_KIND;

    private boolean earlyClocks = true;

    public static final long INFINITE_DURATION = 0;

    public static final long EXPIRATION_TIME_FROM_MESSAGE = -1;

    public static final long DEFAULT_DURATION = INFINITE_DURATION;

    public static final long MAX_EXPIRATION_TIME = Long.MAX_VALUE;

    public static final int MENSUREMENT_TIMESTAMP_KIND = 0;

    public static final int PUBLICATION_TIMESTAMP_KIND = 1;

    public static final int RECEPTION_TIMESTAMP_KIND = 2;

    public static final int DEFAULT_TIMESTAMP_KIND = RECEPTION_TIMESTAMP_KIND;

    public long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime) throws IllegalArgumentException {

        if (expirationTime > Long.MAX_VALUE || expirationTime < 0) {
            throw new IllegalArgumentException("O valor não pode ser maior que " + Long.MAX_VALUE + " e nem menor que " + 0);
        }

        this.expirationTime = expirationTime;
    }

    public int getTimestampKind() {
        return timestampKind;
    }

    public void setKind(int timestampKind) throws IllegalArgumentException {

        if (timestampKind > RECEPTION_TIMESTAMP_KIND || timestampKind < MENSUREMENT_TIMESTAMP_KIND) {
            throw new IllegalArgumentException("O valor não pode ser maior que " + Long.MAX_VALUE + " e nem menor que " + MENSUREMENT_TIMESTAMP_KIND);
        }
        this.timestampKind = timestampKind;
    }

    @Override
    public void restoreDefaultQoS() {
        this.expirationTime = INFINITE_DURATION;
        this.timestampKind = DEFAULT_TIMESTAMP_KIND;
        earlyClocks = true;
    }

    public boolean isEarlyClocks() {
        return earlyClocks;
    }

    public void setEarlyClocks(boolean earlyClocks) {
        this.earlyClocks = earlyClocks;
    }

}
