package br.ufma.lsdi.cddl.pubsub;

/**
 * Created by lcmuniz on 05/03/17.
 */
public final class CDDLFilterImpl implements CDDLFilter {

    private final String eplFilter;

    public CDDLFilterImpl(String eplFilter) {
        this.eplFilter = eplFilter;
    }

    @Override
    public String getEplFilter() {
        return eplFilter;
    }

}
