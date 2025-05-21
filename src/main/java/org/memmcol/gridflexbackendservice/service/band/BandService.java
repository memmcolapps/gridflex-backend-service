package org.memmcol.gridflexbackendservice.service.band;

import org.memmcol.gridflexbackendservice.model.band.Band;

import java.util.Map;
import java.util.UUID;

public interface BandService {
    Map<String, Object> createBand(Band band);

    Map<String, Object> updateBand(Band band);

    Map<String, Object> getBands();

    Map<String, Object> manageBandState(UUID bandId, Boolean status);

    Map<String, Object> getBand(UUID bandId);
}
