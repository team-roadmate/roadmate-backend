package com.trm.roadmate_backend.dto.walk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeoulApiResponse {
    @JsonProperty("TbTraficWlkNet")
    private TbTraficWlkNet tbTraficWlkNet;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TbTraficWlkNet {
        @JsonProperty("list_total_count")
        private Integer list_total_count;

        @JsonProperty("RESULT")
        private Result RESULT;

        @JsonProperty("row")
        private List<Row> row;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        @JsonProperty("CODE")
        private String CODE;

        @JsonProperty("MESSAGE")
        private String MESSAGE;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row {
        @JsonProperty("NODE_TYPE")
        private String NODE_TYPE;

        @JsonProperty("NODE_WKT")
        private String NODE_WKT;

        @JsonProperty("NODE_ID")
        private String NODE_ID;

        @JsonProperty("NODE_TYPE_CD")
        private String NODE_TYPE_CD;

        @JsonProperty("LNKG_WKT")
        private String LNKG_WKT;

        @JsonProperty("LNKG_ID")
        private String LNKG_ID;

        @JsonProperty("LNKG_TYPE_CD")
        private String LNKG_TYPE_CD;

        @JsonProperty("BGNG_LNKG_ID")
        private String BGNG_LNKG_ID;

        @JsonProperty("END_LNKG_ID")
        private String END_LNKG_ID;

        @JsonProperty("LNKG_LEN")
        private Double LNKG_LEN;

        @JsonProperty("SGG_CD")
        private String SGG_CD;

        @JsonProperty("SGG_NM")
        private String SGG_NM;

        @JsonProperty("EMD_CD")
        private String EMD_CD;

        @JsonProperty("EMD_NM")
        private String EMD_NM;

        @JsonProperty("EXPN_CAR_RD")
        private String EXPN_CAR_RD;

        @JsonProperty("SBWY_NTW")
        private String SBWY_NTW;

        @JsonProperty("BRG")
        private String BRG;

        @JsonProperty("TNL")
        private String TNL;

        @JsonProperty("OVRP")
        private String OVRP;

        @JsonProperty("CRSWK")
        private String CRSWK;

        @JsonProperty("PARK")
        private String PARK;

        @JsonProperty("BLDG")
        private String BLDG;
    }
}