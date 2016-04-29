package com.android.tusharg.twitterdatavisualizer;

/**
 * Created by tushar on 27/04/16.
 */
public class DistanceCalculator {
    /**
     * Calculate get between two sets of lat/long.
     *
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return get in miles.
     */
    public static double get(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        return dist * 60 * 1.1515;
    }

    /**
     * This function converts decimal degrees to radians
     *
     * @param deg
     * @return
     */
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /**
     * This function converts radians to decimal degrees
     *
     * @param rad
     * @return
     */
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }
}
