package app.hub.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class for location mapping
 * Makes it easier to add new locations without modifying the main logic
 */
public class LocationConfig {
    
    public static class LocationArea {
        public final String name;
        public final double minLat;
        public final double maxLat;
        public final double minLng;
        public final double maxLng;
        
        public LocationArea(String name, double minLat, double maxLat, double minLng, double maxLng) {
            this.name = name;
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLng = minLng;
            this.maxLng = maxLng;
        }
        
        public boolean contains(double latitude, double longitude) {
            return latitude >= minLat && latitude <= maxLat && 
                   longitude >= minLng && longitude <= maxLng;
        }
    }
    
    /**
     * List of all configured location areas
     * Add new locations here instead of modifying the main code
     */
    public static List<LocationArea> getLocationAreas() {
        List<LocationArea> areas = new ArrayList<>();
        
        // Metro Manila Cities
        areas.add(new LocationArea("Manila City", 14.55, 14.65, 120.95, 121.05));
        areas.add(new LocationArea("Makati City", 14.52, 14.58, 121.00, 121.05));
        areas.add(new LocationArea("Taguig City", 14.48, 14.55, 121.03, 121.10));
        areas.add(new LocationArea("Pasay City", 14.50, 14.55, 120.98, 121.02));
        areas.add(new LocationArea("Mandaluyong City", 14.56, 14.60, 121.02, 121.06));
        areas.add(new LocationArea("San Juan City", 14.58, 14.62, 121.02, 121.05));
        areas.add(new LocationArea("Quezon City", 14.60, 14.70, 121.00, 121.10));
        areas.add(new LocationArea("Pasig City", 14.55, 14.60, 121.05, 121.12));
        areas.add(new LocationArea("Marikina City", 14.62, 14.68, 121.08, 121.15));
        areas.add(new LocationArea("Caloocan City", 14.70, 14.80, 120.95, 121.02));
        areas.add(new LocationArea("Valenzuela City", 14.65, 14.72, 120.93, 121.00));
        areas.add(new LocationArea("Las Piñas City", 14.42, 14.48, 120.97, 121.02));
        areas.add(new LocationArea("Parañaque City", 14.43, 14.48, 121.00, 121.04));
        areas.add(new LocationArea("Muntinlupa City", 14.35, 14.42, 121.02, 121.08));
        areas.add(new LocationArea("Navotas City", 14.62, 14.67, 120.92, 120.97));
        areas.add(new LocationArea("Malabon City", 14.64, 14.69, 120.92, 120.97));
        
        // Rizal Province Cities
        areas.add(new LocationArea("Antipolo City", 14.55, 14.62, 121.15, 121.20));
        areas.add(new LocationArea("San Mateo", 14.67, 14.72, 121.10, 121.15));
        areas.add(new LocationArea("Cainta", 14.56, 14.61, 121.10, 121.13));
        
        // Add more locations here as needed
        // Major Philippine Cities
        areas.add(new LocationArea("Cebu City", 10.25, 10.38, 123.82, 123.95));
        areas.add(new LocationArea("Davao City", 7.05, 7.15, 125.55, 125.65));
        areas.add(new LocationArea("Baguio City", 16.38, 16.43, 120.57, 120.62));
        areas.add(new LocationArea("Iloilo City", 10.68, 10.73, 122.53, 122.58));
        areas.add(new LocationArea("Cagayan de Oro", 8.45, 8.50, 124.63, 124.68));
        areas.add(new LocationArea("Zamboanga City", 6.88, 6.93, 122.05, 122.10));
        areas.add(new LocationArea("Bacolod City", 10.63, 10.68, 122.93, 122.98));
        areas.add(new LocationArea("General Santos", 6.08, 6.13, 125.15, 125.20));
        areas.add(new LocationArea("Tacloban City", 11.23, 11.28, 124.98, 125.03));
        areas.add(new LocationArea("Butuan City", 8.93, 8.98, 125.53, 125.58));
        
        // Luzon Cities
        areas.add(new LocationArea("Angeles City", 15.13, 15.18, 120.58, 120.63));
        areas.add(new LocationArea("San Fernando Pampanga", 15.03, 15.08, 120.68, 120.73));
        areas.add(new LocationArea("Olongapo City", 14.80, 14.85, 120.27, 120.32));
        areas.add(new LocationArea("Batangas City", 13.73, 13.78, 121.03, 121.08));
        areas.add(new LocationArea("Lipa City", 13.93, 13.98, 121.15, 121.20));
        areas.add(new LocationArea("San Pablo City", 14.05, 14.10, 121.32, 121.37));
        areas.add(new LocationArea("Lucena City", 13.93, 13.98, 121.60, 121.65));
        areas.add(new LocationArea("Naga City", 13.60, 13.65, 123.18, 123.23));
        areas.add(new LocationArea("Legazpi City", 13.13, 13.18, 123.73, 123.78));
        
        // Regional fallbacks (broader areas)
        areas.add(new LocationArea("Metro Manila Area", 14.3, 14.8, 120.9, 121.2));
        areas.add(new LocationArea("Rizal Province", 14.4, 14.8, 121.0, 121.3));
        areas.add(new LocationArea("Cavite Province", 14.1, 14.5, 120.6, 121.1));
        areas.add(new LocationArea("Laguna Province", 14.0, 14.4, 121.0, 121.6));
        areas.add(new LocationArea("Bulacan Province", 14.6, 15.0, 120.7, 121.2));
        areas.add(new LocationArea("Cebu Province", 9.8, 11.3, 123.3, 124.1));
        areas.add(new LocationArea("Davao Region", 6.5, 8.0, 125.0, 126.5));
        
        return areas;
    }
    
    /**
     * Find location name for given coordinates
     */
    public static String findLocationName(double latitude, double longitude) {
        List<LocationArea> areas = getLocationAreas();
        
        // Check each area in order (more specific areas should be listed first)
        for (LocationArea area : areas) {
            if (area.contains(latitude, longitude)) {
                return area.name;
            }
        }
        
        // Default fallback
        return "Philippines";
    }
}
