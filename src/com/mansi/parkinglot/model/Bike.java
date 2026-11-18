package com.mansi.parkinglot.model;

public class Bike extends Vehicle {
    public Bike(String registrationNumber) {
        super(registrationNumber);
    }

    @Override
    public VehicleType getType() {
        return VehicleType.BIKE;
    }
}
