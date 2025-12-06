package com.trm.roadmate_backend.dto;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

@Data
public class ApiResponse {
    @SerializedName("TbTraficWlkNet")
    private TbTraficWlkNet tbTraficWlkNet;

    @Data
    public static class TbTraficWlkNet {
        @SerializedName("list_total_count")
        private int listTotalCount; // 전체 데이터 수

        @SerializedName("RESULT")
        private Result result; // API 호출 결과

        @SerializedName("row")
        private List<Row> row; // 실제 데이터 목록
    }

    @Data
    public static class Result {
        @SerializedName("CODE")
        private String code;

        @SerializedName("MESSAGE")
        private String message;
    }

    @Data
    public static class Row {
        @SerializedName("NODE_TYPE")
        private String nodeType; // 'NODE' 또는 'LINK'

        @SerializedName("NODE_WKT")
        private String nodeWkt; // 노드 WKT (POINT(lon lat))

        @SerializedName("NODE_ID")
        private String nodeId; // 노드 ID

        @SerializedName("NODE_TYPE_CD")
        private String nodeTypeCd; // 노드 유형 코드

        @SerializedName("LNKG_WKT")
        private String lnkgWkt; // 링크 WKT (LINESTRING(lon1 lat1, lon2 lat2, ...))

        @SerializedName("LNKG_ID")
        private String lnkgId; // 링크 ID

        @SerializedName("LNKG_TYPE_CD")
        private String lnkgTypeCd; // 링크 유형 코드

        @SerializedName("BGNG_LNKG_ID")
        private String bgngLnkgId; // 시작 노드 ID

        @SerializedName("END_LNKG_ID")
        private String endLnkgId; // 끝 노드 ID

        @SerializedName("LNKG_LEN")
        private Double lnkgLen; // 링크 길이

        @SerializedName("SGG_CD")
        private String sggCd;

        @SerializedName("SGG_NM")
        private String sggNm;

        @SerializedName("EMD_CD")
        private String emdCd;

        @SerializedName("EMD_NM")
        private String emdNm;

        // 링크 속성 정보 (길/시설물 여부)
        @SerializedName("EXPN_CAR_RD")
        private String expnCarRd;

        @SerializedName("SBWY_NTW")
        private String sbwyNtw;

        @SerializedName("BRG")
        private String brg;

        @SerializedName("TNL")
        private String tnl;

        @SerializedName("OVRP")
        private String ovrp;

        @SerializedName("CRSWK")
        private String crswk;

        @SerializedName("PARK")
        private String park;

        @SerializedName("BLDG")
        private String bldg;
    }
}