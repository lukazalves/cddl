package br.ufma.lsdi.cddl.qos;

public final class LatencyBudgetQoS extends AbstractQoS {

    private long delay = DEFAULT_DELAY;

    public static final long DEFAULT_DELAY = 0;

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {

        if (delay > Long.MAX_VALUE || delay < 0) {
            throw new IllegalArgumentException("O valor não pode ser maior que " + Long.MAX_VALUE + " e nem menor que " + DEFAULT_DELAY);
        }

        this.delay = delay;
    }

    @Override
    public void restoreDefaultQoS() {
        this.delay = DEFAULT_DELAY;
    }

}
