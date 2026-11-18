package com.mansi.parkinglot.service;

import com.mansi.parkinglot.model.VehicleType;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Calculates parking charges.
 * Duration is rounded up to the next full hour, with a minimum of 1 hour,
 * so even a short stay is billed as one hour.
 *
 * Rates (INR per hour): Bike Rs 20, Car Rs 40, Truck Rs 80.
 */
public class FeeCalculator {

    public int billableHours(LocalDateTime entry, LocalDateTime exit) {
        if (exit.isBefore(entry)) {
            throw new IllegalArgumentException("Exit time cannot be before entry time.");
        }
        long minutes = Duration.between(entry, exit).toMinutes();
        if (minutes <= 0) {
            return 1; // same-minute exit still counts as one hour
        }
        long hours = minutes / 60;
        if (minutes % 60 != 0) {
            hours += 1;
        }
        return (int) Math.max(hours, 1);
    }

    public double calculate(VehicleType type, LocalDateTime entry, LocalDateTime exit) {
        int hours = billableHours(entry, exit);
        return hours * type.getHourlyRate();
    }

    public String describeRates() {
        StringBuilder sb = new StringBuilder("Hourly rates (rounded up to the next hour):\n");
        for (VehicleType type : VehicleType.values()) {
            sb.append(String.format("  %-6s  Rs %.0f / hour%n", type, type.getHourlyRate()));
        }
        return sb.toString();
    }
}
