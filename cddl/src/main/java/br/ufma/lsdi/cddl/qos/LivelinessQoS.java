package br.ufma.lsdi.cddl.qos;


public final class LivelinessQoS extends AbstractQoS {

    private int kind = DEFAULT_KIND;

    private long leaseDuration = DEFAULT_LEASE_DURANTION;

    private DurabilityQoS durabilityQoS = new DurabilityQoS();

    private ReliabilityQoS reliabilityQoS = new ReliabilityQoS();

    public static final int AUTOMATIC = 0;

    public static final int MANUAL = 1;

    public static final int DEFAULT_KIND = MANUAL;

    public static long DEFAULT_LEASE_DURANTION = 0;

    public int getKind() {
        return kind;
    }

    public void setkind(int kind) throws IllegalArgumentException {

        if (kind > MANUAL || kind < AUTOMATIC) {
            throw new IllegalArgumentException("O valor não pode ser maior que " + MANUAL + " e nem menor que " + AUTOMATIC);
        }

        this.kind = kind;
    }

    public long getLeaseDuration() {
        return leaseDuration;
    }


    public void setLeaseDuration(long leaseDuration) throws IllegalArgumentException {

        if (leaseDuration > Long.MAX_VALUE || leaseDuration < 0) {
            throw new IllegalArgumentException("O valor não pode ser maior que " + Long.MAX_VALUE + " e nem menor que zero");
        }

        this.leaseDuration = leaseDuration;
    }

    @Override
    public void restoreDefaultQoS() {
        this.kind = DEFAULT_KIND;
        this.leaseDuration = DEFAULT_LEASE_DURANTION;
    }

    public DurabilityQoS getDurabilityQoS() {
        return durabilityQoS;
    }


    public void setDurabilityQoS(DurabilityQoS durabilityQoS) {
        this.durabilityQoS = durabilityQoS;
    }

    public ReliabilityQoS getReliabilityQoS() {
        return reliabilityQoS;
    }

    public void setReliabilityQoS(ReliabilityQoS reliabilityQoS) {
        this.reliabilityQoS = reliabilityQoS;
    }

}
