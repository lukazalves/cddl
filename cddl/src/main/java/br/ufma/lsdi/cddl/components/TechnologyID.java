package br.ufma.lsdi.cddl.components;

public enum TechnologyID {

    BLE(1), BT(0), INTERNAL(100);

    public final int id;

    TechnologyID(int id) {
        this.id = id;
    }
}
