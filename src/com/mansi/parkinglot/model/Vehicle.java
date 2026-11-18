package com.mansi.parkinglot.model;

import com.mansi.parkinglot.exception.InvalidInputException;

/**
 * Abstract base class for every vehicle that can enter the lot.
 * Concrete types (Bike, Car, Truck) only differ by their {@link VehicleType}.
 */
public abstract class Vehicle {
    private final String registrationNumber;

    protected Vehicle(String registrationNumber) {
        this.registrationNumber = registrationNumber == null
                ? ""
                : registrationNumber.trim().toUpperCase();
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public abstract VehicleType getType();

    public SlotSize getRequiredSize() {
        return getType().getRequiredSize();
    }

    /**
     * Factory that builds the correct subclass from a type and registration number.
     */
    public static Vehicle create(VehicleType type, String registrationNumber)
            throws InvalidInputException {
        if (type == null) {
            throw new InvalidInputException("Please choose a valid vehicle type (Bike, Car or Truck).");
        }
        switch (type) {
            case BIKE:
                return new Bike(registrationNumber);
            case CAR:
                return new Car(registrationNumber);
            case TRUCK:
                return new Truck(registrationNumber);
            default:
                throw new InvalidInputException("Unsupported vehicle type: " + type);
        }
    }

    @Override
    public String toString() {
        return getType() + " (" + registrationNumber + ")";
    }
}
