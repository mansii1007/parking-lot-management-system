package com.mansi.parkinglot.exception;

/** Base checked exception for every parking-lot error shown to the user. */
public class ParkingLotException extends Exception {
    public ParkingLotException(String message) {
        super(message);
    }
}
