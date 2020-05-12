package br.ufma.lsdi.cddl.qos;


public abstract class AbstractQoS {

    public String getName() {
        return getClass().getSimpleName();
    }

    public abstract void restoreDefaultQoS();

}
