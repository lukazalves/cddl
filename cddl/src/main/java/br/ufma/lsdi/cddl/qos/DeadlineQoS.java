package br.ufma.lsdi.cddl.qos;


public final class DeadlineQoS extends AbstractQoS {

    private long period = DEFAULT_DEADLINE;

    public static final long DEFAULT_DEADLINE = 0;

    public long getPeriod() {
        return period;
    }

    public void setPeriod(long period) {
        if (period > Long.MAX_VALUE || period < DEFAULT_DEADLINE) {
            throw new IllegalArgumentException("O valor não pode ser menor que " + DEFAULT_DEADLINE + "e nem maior que " + Long.MAX_VALUE);
        }
        this.period = period;
    }

    @Override
    public synchronized void restoreDefaultQoS() {
        this.period = DEFAULT_DEADLINE;
    }


}
