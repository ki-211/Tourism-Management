package com.zkt.backend.common;

import java.math.BigDecimal;

/** Great-circle distance; both points must use the same coordinate system (the app uses GCJ-02 everywhere). */
public final class Geo {
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private Geo() {}

    public static double distanceMeters(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double phi1 = Math.toRadians(lat1.doubleValue()), phi2 = Math.toRadians(lat2.doubleValue());
        double dPhi = phi2 - phi1, dLambda = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        double h = Math.pow(Math.sin(dPhi / 2), 2) + Math.cos(phi1) * Math.cos(phi2) * Math.pow(Math.sin(dLambda / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(h)));
    }
}
