package app.hub.util;

/**
 * Utility class for location-related constants and helper methods
 */
public class LocationUtils {
    
    // Philippines geographic boundaries
    public static final double PHILIPPINES_MIN_LAT = 4.0;
    public static final double PHILIPPINES_MAX_LAT = 21.0;
    public static final double PHILIPPINES_MIN_LNG = 116.0;
    public static final double PHILIPPINES_MAX_LNG = 127.0;
    
    /**
     * Checks if a given latitude and longitude are within Philippine boundaries
     *
     * @param latitude Latitude to check
     * @param longitude Longitude to check
     * @return true if coordinates are within Philippine boundaries, false otherwise
     */
    public static boolean isWithinPhilippines(double latitude, double longitude) {
        return latitude >= PHILIPPINES_MIN_LAT && 
               latitude <= PHILIPPINES_MAX_LAT &&
               longitude >= PHILIPPINES_MIN_LNG && 
               longitude <= PHILIPPINES_MAX_LNG;
    }
}
