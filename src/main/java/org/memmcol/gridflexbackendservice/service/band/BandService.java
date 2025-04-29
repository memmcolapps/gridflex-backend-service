package org.memmcol.gridflexbackendservice.service.band;

import org.memmcol.gridflexbackendservice.model.Band;

import java.util.Map;

public interface BandService {
    Map<String, Object> createBand(Band band);

    Map<String, Object> updateBand(Band band);

    Map<String, Object> getBands();

    Map<String, Object> disableBand(Long bandId,Boolean status);
}
