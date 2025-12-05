package com.trm.roadmate_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class SeoulApiResponse {

    @JsonProperty("TbTraficWlkNet")
    private TbTraficWlkNet tbTraficWlkNet;

    @Data
    public static class TbTraficWlkNet {
        @JsonProperty("list_total_count")
        private Integer listTotalCount;

        @JsonProperty("RESULT")
        private Result result;

        @JsonProperty("row")
        private List<NetworkRow> row;
    }

    @Data
    public static class Result {
        @JsonProperty("CODE")
        private String code;

        @JsonProperty("MESSAGE")
        private String message;
    }

    @Data
    public static class NetworkRow {
        // 공통 필드
        @JsonProperty("NODE_TYPE")
        private String nodeType;  // "NODE" 또는 "LINK"

        @JsonProperty("SGG_CD")
        private String sggCd;

        @JsonProperty("SGG_NM")
        private String sggNm;

        @JsonProperty("EMD_CD")
        private String emdCd;

        @JsonProperty("EMD_NM")
        private String emdNm;

        // 노드 전용 필드
        @JsonProperty("NODE_ID")
        private String nodeId;

        @JsonProperty("NODE_TYPE_CD")
        private String nodeTypeCd;

        @JsonProperty("NODE_WKT")
        private String nodeWkt;

        // 링크 전용 필드
        @JsonProperty("LNKG_ID")
        private String lnkgId;

        @JsonProperty("LNKG_TYPE_CD")
        private String lnkgTypeCd;

        @JsonProperty("LNKG_WKT")
        private String lnkgWkt;

        @JsonProperty("BGNG_LNKG_ID")
        private String bgngLnkgId;  // 시작노드 ID

        @JsonProperty("END_LNKG_ID")
        private String endLnkgId;   // 종료노드 ID

        @JsonProperty("LNKG_LEN")
        private String lnkgLen;

        // 부가 정보
        @JsonProperty("EXPN_CAR_RD")
        private String expnCarRd;

        @JsonProperty("SBWY_NTW")
        private String sbwyNtw;

        @JsonProperty("BRG")
        private String brg;

        @JsonProperty("TNL")
        private String tnl;

        @JsonProperty("OVRP")
        private String ovrp;

        @JsonProperty("CRSWK")
        private String crswk;

        @JsonProperty("PARK")
        private String park;

        @JsonProperty("BLDG")
        private String bldg;
    }
}