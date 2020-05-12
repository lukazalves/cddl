package br.ufma.lsdi.cddl.listeners;

/**
 * Created by bertodetacio on 28/05/17.
 */

public interface IConnectionListener {

    void onConnectionEstablished();

    void onConnectionEstablishmentFailed();

    void onConnectionLost();

    void onDisconnectedNormally();
}
