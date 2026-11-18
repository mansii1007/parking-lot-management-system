package com.mansi.parkinglot.model;

/**
 * A single parking bay on a floor.
 * The slot id looks like F0-S-03 (floor, size initial, running number).
 */
public class ParkingSlot {
    private final String id;
    private final int floorNumber;
    private final SlotSize size;
    private String occupiedByTicketId; // null when free

    public ParkingSlot(String id, int floorNumber, SlotSize size) {
        this.id = id;
        this.floorNumber = floorNumber;
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public SlotSize getSize() {
        return size;
    }

    public boolean isFree() {
        return occupiedByTicketId == null;
    }

    public String getOccupiedByTicketId() {
        return occupiedByTicketId;
    }

    public void occupy(String ticketId) {
        this.occupiedByTicketId = ticketId;
    }

    public void vacate() {
        this.occupiedByTicketId = null;
    }

    @Override
    public String toString() {
        return id + " (" + size + (isFree() ? ", free" : ", occupied") + ")";
    }
}
