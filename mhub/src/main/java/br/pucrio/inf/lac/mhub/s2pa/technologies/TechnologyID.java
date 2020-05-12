package br.pucrio.inf.lac.mhub.s2pa.technologies;

public enum TechnologyID {

    BLE(1), BT(0), INTERNAL(100);

    private final int i;

    TechnologyID(int i) {
        this.i = i;
    }
}
