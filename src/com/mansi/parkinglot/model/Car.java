package com.mansi.parkinglot.model;

public class Car extends Vehicle {
    public Car(String registrationNumber) {
        super(registrationNumber);
    }

    @Override
    public VehicleType getType() {
        return VehicleType.CAR;
    }
}
