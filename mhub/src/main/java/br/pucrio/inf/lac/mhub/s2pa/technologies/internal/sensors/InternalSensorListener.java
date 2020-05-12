package br.pucrio.inf.lac.mhub.s2pa.technologies.internal.sensors;

import br.pucrio.inf.lac.mhub.models.locals.SensorData;

public interface InternalSensorListener {

    public void onInternalSensorChanged(SensorData data);

}
