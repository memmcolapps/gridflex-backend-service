package org.memmcol.gridflexbackendservice.model.hes;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class DlmsBulkRequest {

    @Data
    public static class SetClockRequest {
        private List<String> serials;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime dateTime;
    }

    @Data
    public static class SetCtptRequest {
        private List<String> serials;
        private long ctNumerator;
        private long ctDenominator;
        private long ptNumerator;
        private long ptDenominator;
    }

    @Data
    public static class SetApnRequest {
        private List<String> serials;
        private String apn;
    }

    @Data
    public static class SetIpPortRequest {
        private List<String> serials;
        private String ip;
        private int port;
    }
}
