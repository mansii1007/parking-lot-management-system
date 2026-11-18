package com.mansi.parkinglot.exception;

public class VehicleAlreadyParkedException extends ParkingLotException {
    public VehicleAlreadyParkedException(String message) {
        super(message);
    }
}
