package br.ufma.lsdi.cddl.qos;

public final class TimeBasedFilterQoS extends AbstractQoS {

    private long minSeparation = DEFAULT_MIN_SEPARATION_INTERVAL;

    public static final long DEFAULT_MIN_SEPARATION_INTERVAL = 0;

    public long getMinSeparation() {
        return minSeparation;
    }

    public void setMinSeparation(long minSeparation) {
        if (minSeparation > Long.MAX_VALUE || minSeparation < DEFAULT_MIN_SEPARATION_INTERVAL) {
            throw new IllegalArgumentException("O valor deve não pode ser maior que " + Long.MAX_VALUE + " e nem menor que " + DEFAULT_MIN_SEPARATION_INTERVAL);
        }
        this.minSeparation = minSeparation;
    }

    @Override
    public void restoreDefaultQoS() {
        this.minSeparation = DEFAULT_MIN_SEPARATION_INTERVAL;
    }

}
