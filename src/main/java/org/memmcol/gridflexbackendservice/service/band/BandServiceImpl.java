package org.memmcol.gridflexbackendservice.service.band;

import org.memmcol.gridflexbackendservice.mapper.BandMapper;
import org.memmcol.gridflexbackendservice.model.Band;
import org.memmcol.gridflexbackendservice.util.ResponseMap;
import org.memmcol.gridflexbackendservice.util.ResponseProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BandServiceImpl implements BandService {
    @Autowired
    private BandMapper bandMapper;

    @Autowired
    private ResponseProperties status;

    private String bd = "Band";

    @Override
    public Map<String, Object> createBand(Band band) {
        try {
            String isExist = bandMapper.getBand(band.getName());
            if (isExist != null) {
                return ResponseMap.response(status.getExistCode(), bd + " " + status.getExistDesc(), "");
            }
            int result = bandMapper.createBand(band);
            if(result == 0){
                return ResponseMap.response(status.getRegCode(), bd + " " + status.getRegFailureDesc(), "");
            }
            return ResponseMap.response(status.getSuccessCode(), bd + " " + status.getRegDesc(), "");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw e;
        }

    }

    @Override
    public Map<String, Object> updateBand(Band band) {
        try {
            String isExist = bandMapper.getBand(band.getName());
            if (isExist == null) {
                return ResponseMap.response(status.getNotFoundCode(), bd + " " + status.getNotFoundDesc(), "");
            }
            int result = bandMapper.updateBand(band);
            if(result == 0){
                return ResponseMap.response(status.getUpdateCode(), bd + " " + status.getUpdateFailureDesc(), "");
            }
            return ResponseMap.response(status.getSuccessCode(), bd + " " + status.getUpdateDesc(), "");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    @Override
    public Map<String, Object> getBands() {
        try {
            List<Band> result = bandMapper.fetchBands();
            if(result == null) {
                return ResponseMap.response(status.getNotFoundCode(), bd + " " + status.getNotFoundDesc(), "");
            }
            return ResponseMap.response(status.getSuccessCode(), bd + " " + status.getDesc(), result);
        } catch (Exception e) {
            throw e;
        }
    }
}
