package com.mansi.parkinglot.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Issued when a vehicle is parked. The same object is updated on exit
 * with the leave time and the fee that was charged.
 */
public class Ticket {
    public static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String ticketId;
    private final String registrationNumber;
    private final VehicleType vehicleType;
    private final String slotId;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private double fee;
    private TicketStatus status;

    public Ticket(String ticketId, String registrationNumber, VehicleType vehicleType,
                  String slotId, LocalDateTime entryTime) {
        this.ticketId = ticketId;
        this.registrationNumber = registrationNumber;
        this.vehicleType = vehicleType;
        this.slotId = slotId;
        this.entryTime = entryTime;
        this.status = TicketStatus.ACTIVE;
        this.fee = 0.0;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public String getSlotId() {
        return slotId;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public double getFee() {
        return fee;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void complete(LocalDateTime exitTime, double fee) {
        this.exitTime = exitTime;
        this.fee = fee;
        this.status = TicketStatus.COMPLETED;
    }

    public String formatEntryTime() {
        return entryTime.format(TIME_FORMAT);
    }

    public String formatExitTime() {
        return exitTime == null ? "-" : exitTime.format(TIME_FORMAT);
    }

    /**
     * A readable receipt used both in the console and in the saved data file.
     */
    public String toReceipt() {
        StringBuilder sb = new StringBuilder();
        sb.append("----------------------------------------\n");
        sb.append(" Ticket ID     : ").append(ticketId).append('\n');
        sb.append(" Vehicle       : ").append(vehicleType).append("  ").append(registrationNumber).append('\n');
        sb.append(" Slot          : ").append(slotId).append('\n');
        sb.append(" Entry time    : ").append(formatEntryTime()).append('\n');
        sb.append(" Exit time     : ").append(formatExitTime()).append('\n');
        sb.append(" Status        : ").append(status).append('\n');
        if (status == TicketStatus.COMPLETED) {
            sb.append(String.format(" Fee charged   : Rs %.2f%n", fee));
        } else {
            sb.append(" Fee charged   : (payable at exit)\n");
        }
        sb.append("----------------------------------------");
        return sb.toString();
    }

    @Override
    public String toString() {
        return ticketId + " | " + vehicleType + " " + registrationNumber
                + " | slot " + slotId + " | in " + formatEntryTime();
    }
}
