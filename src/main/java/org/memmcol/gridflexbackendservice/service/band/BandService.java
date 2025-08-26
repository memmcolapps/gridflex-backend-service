package org.memmcol.gridflexbackendservice.service.band;

import org.memmcol.gridflexbackendservice.model.band.Band;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Map;
import java.util.UUID;

public interface BandService {
    Map<String, Object> createBand(Band band);

    Map<String, Object> updateBand(Band band);

    Map<String, Object> getBands(String type);

    Map<String, Object> approve(UUID bandId, String approveStatus) throws MissingServletRequestParameterException;

    Map<String, Object> getBand(UUID bandId, UUID bandVersionId);
}
