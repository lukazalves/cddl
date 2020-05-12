package br.ufma.lsdi.cddl.qos;

public final class DurabilityQoS extends AbstractQoS {

    private int kind = VOLATILE;

    public static final int VOLATILE = 0;

    public static final int PERSISTENT = 1;

    public static final int DEFAULT_KIND = VOLATILE;

    public int getKind() {
        return kind;
    }

    public void setKind(int kind) throws IllegalArgumentException {

        if (kind > PERSISTENT || kind < VOLATILE) {
            throw new IllegalArgumentException("O valor não pode ser menor que " + VOLATILE + "e nem maior que " + PERSISTENT);
        }

        this.kind = kind;
    }

    @Override
    public void restoreDefaultQoS() {
        this.kind = DEFAULT_KIND;
    }

    public boolean isRetained() {
        if (kind == PERSISTENT) {
            return true;
        } else {
            return false;
        }
    }

}
