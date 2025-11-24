package com.trm.roadmate_backend.util;

/**
 * 위경도 좌표를 기상청 격자 좌표(X, Y)로 변환하는 유틸리티
 * 기상청 Lambert Conformal Conic Projection 방식 사용
 */
public class GridConverter {

    private static final double RE = 6371.00877; // 지구 반경(km)
    private static final double GRID = 5.0;      // 격자 간격(km)
    private static final double SLAT1 = 30.0;    // 표준 위도1
    private static final double SLAT2 = 60.0;    // 표준 위도2
    private static final double OLON = 126.0;    // 기준점 경도
    private static final double OLAT = 38.0;     // 기준점 위도
    private static final double XO = 43;         // 기준점 X좌표(격자)
    private static final double YO = 136;        // 기준점 Y좌표(격자)

    private static final double DEGRAD = Math.PI / 180.0;
    private static final double re = RE / GRID;
    private static final double slat1 = SLAT1 * DEGRAD;
    private static final double slat2 = SLAT2 * DEGRAD;
    private static final double olon = OLON * DEGRAD;
    private static final double olat = OLAT * DEGRAD;

    private static double sn;
    private static double sf;
    private static double ro;

    static {
        sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);
    }

    /**
     * 위경도 -> 격자 좌표 변환
     * @param lat 위도
     * @param lon 경도
     * @return [nx, ny] 격자 좌표
     */
    public static int[] toGrid(double lat, double lon) {
        double ra = Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);

        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        double x = Math.floor(ra * Math.sin(theta) + XO + 0.5);
        double y = Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return new int[]{(int) x, (int) y};
    }

    /**
     * 격자 좌표 -> 위경도 변환 (참고용)
     * @param nx X 격자
     * @param ny Y 격자
     * @return [lat, lon] 위경도
     */
    public static double[] toLatLon(int nx, int ny) {
        double xn = nx - XO;
        double yn = ro - ny + YO;
        double ra = Math.sqrt(xn * xn + yn * yn);
        if (sn < 0.0) ra = -ra;

        double alat = Math.pow((re * sf / ra), (1.0 / sn));
        alat = 2.0 * Math.atan(alat) - Math.PI * 0.5;

        double theta;
        if (Math.abs(xn) <= 0.0) {
            theta = 0.0;
        } else {
            if (Math.abs(yn) <= 0.0) {
                theta = Math.PI * 0.5;
                if (xn < 0.0) theta = -theta;
            } else {
                theta = Math.atan2(xn, yn);
            }
        }
        double alon = theta / sn + olon;

        double lat = alat / DEGRAD;
        double lon = alon / DEGRAD;

        return new double[]{lat, lon};
    }
}