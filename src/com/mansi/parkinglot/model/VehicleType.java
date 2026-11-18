package com.mansi.parkinglot.model;

/**
 * Supported vehicle types in the parking lot.
 * Each type has a required slot size and an hourly parking rate (in INR).
 */
public enum VehicleType {
    BIKE(SlotSize.SMALL, 20.0),
    CAR(SlotSize.MEDIUM, 40.0),
    TRUCK(SlotSize.LARGE, 80.0);

    private final SlotSize requiredSize;
    private final double hourlyRate;

    VehicleType(SlotSize requiredSize, double hourlyRate) {
        this.requiredSize = requiredSize;
        this.hourlyRate = hourlyRate;
    }

    public SlotSize getRequiredSize() {
        return requiredSize;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    /**
     * Parses a menu choice or name such as "1", "bike", or "CAR".
     */
    public static VehicleType fromInput(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim().toUpperCase();
        switch (value) {
            case "1":
            case "BIKE":
            case "B":
                return BIKE;
            case "2":
            case "CAR":
            case "C":
                return CAR;
            case "3":
            case "TRUCK":
            case "T":
            case "LORRY":
                return TRUCK;
            default:
                return null;
        }
    }
}
