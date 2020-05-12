package br.ufma.lsdi.cddl.qos;

public final class ReliabilityQoS extends AbstractQoS {

    public static final int AT_MOST_ONCE = 0;

    public static final int AT_LEAST_ONCE = 1;

    public static final int EXACTLY_ONCE = 2;

    public static final int DEFAULT_KIND = AT_MOST_ONCE;

    public int kind = DEFAULT_KIND;

    public ReliabilityQoS() {
    }

    public ReliabilityQoS(int kind) {
        super();
        setKind(kind);
    }

    public int getKind() {
        return kind;
    }

    public void setKind(int kind) {

        if (kind > EXACTLY_ONCE || kind < AT_MOST_ONCE) {
            throw new IllegalArgumentException("O valor não pode ser maior que " + EXACTLY_ONCE + " e nem menor que " + AT_MOST_ONCE);
        }

        this.kind = kind;
    }

    @Override
    public void restoreDefaultQoS() {
        this.kind = DEFAULT_KIND;
    }

}
