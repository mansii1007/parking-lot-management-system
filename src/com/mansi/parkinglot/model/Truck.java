package com.mansi.parkinglot.model;

public class Truck extends Vehicle {
    public Truck(String registrationNumber) {
        super(registrationNumber);
    }

    @Override
    public VehicleType getType() {
        return VehicleType.TRUCK;
    }
}
