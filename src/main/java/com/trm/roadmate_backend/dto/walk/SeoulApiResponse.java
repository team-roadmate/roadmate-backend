package com.trm.roadmate_backend.dto.walk;

import lombok.*;
import java.util.List;

// ===== 서울시 API 응답 DTO =====
@Data
public class SeoulApiResponse {
    private TbTraficWlkNet TbTraficWlkNet;

    @Data
    public static class TbTraficWlkNet {
        private Integer list_total_count;
        private Result RESULT;
        private List<Row> row;
    }

    @Data
    public static class Result {
        private String CODE;
        private String MESSAGE;
    }

    @Data
    public static class Row {
        private String NODE_TYPE;
        private String NODE_WKT;
        private String NODE_ID;
        private String NODE_TYPE_CD;
        private String LNKG_WKT;
        private String LNKG_ID;
        private String LNKG_TYPE_CD;
        private String BGNG_LNKG_ID;
        private String END_LNKG_ID;
        private Double LNKG_LEN;
        private String SGG_CD;
        private String SGG_NM;
        private String EMD_CD;
        private String EMD_NM;
        private String EXPN_CAR_RD;
        private String SBWY_NTW;
        private String BRG;
        private String TNL;
        private String OVRP;
        private String CRSWK;
        private String PARK;
        private String BLDG;
    }
}
