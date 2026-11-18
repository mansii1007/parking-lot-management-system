package com.mansi.parkinglot.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * One floor of the parking lot, holding a mix of small, medium and large slots.
 */
public class Floor {
    private final int number;
    private final List<ParkingSlot> slots = new ArrayList<>();

    public Floor(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    public void addSlot(ParkingSlot slot) {
        slots.add(slot);
    }

    public List<ParkingSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    /**
     * Finds a free slot that can fit the required size.
     * Exact-size slots are preferred so that larger bays stay free for bigger vehicles.
     */
    public Optional<ParkingSlot> findAvailable(SlotSize required) {
        ParkingSlot fallback = null;
        for (ParkingSlot slot : slots) {
            if (!slot.isFree() || !slot.getSize().canFit(required)) {
                continue;
            }
            if (slot.getSize() == required) {
                return Optional.of(slot);
            }
            if (fallback == null) {
                fallback = slot;
            }
        }
        return Optional.ofNullable(fallback);
    }

    public long countFree(SlotSize size) {
        return slots.stream().filter(s -> s.getSize() == size && s.isFree()).count();
    }

    public long countTotal(SlotSize size) {
        return slots.stream().filter(s -> s.getSize() == size).count();
    }

    public String displayName() {
        return number == 0 ? "Ground Floor (F0)" : "Floor " + number + " (F" + number + ")";
    }
}
