package com.mansi.parkinglot.model;

/**
 * Physical size of a parking slot.
 * A vehicle may park in a slot of its own size or any larger size
 * (e.g. a bike can use a medium or large slot if no small slot is free).
 */
public enum SlotSize {
    SMALL,
    MEDIUM,
    LARGE;

    /**
     * Returns true if this slot is big enough for a vehicle that needs {@code required}.
     */
    public boolean canFit(SlotSize required) {
        return this.ordinal() >= required.ordinal();
    }
}
