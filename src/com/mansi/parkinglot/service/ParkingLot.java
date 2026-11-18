package com.mansi.parkinglot.service;

import com.mansi.parkinglot.exception.InvalidInputException;
import com.mansi.parkinglot.exception.InvalidTicketException;
import com.mansi.parkinglot.exception.SlotUnavailableException;
import com.mansi.parkinglot.exception.VehicleAlreadyParkedException;
import com.mansi.parkinglot.model.Floor;
import com.mansi.parkinglot.model.ParkingSlot;
import com.mansi.parkinglot.model.SlotSize;
import com.mansi.parkinglot.model.Ticket;
import com.mansi.parkinglot.model.TicketStatus;
import com.mansi.parkinglot.model.Vehicle;
import com.mansi.parkinglot.model.VehicleType;
import com.mansi.parkinglot.util.VehicleNumberValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Core parking-lot logic: allocating slots, issuing tickets, unparking,
 * availability views and the daily revenue report.
 *
 * This class does not print to the console; the menu in {@code ParkingLotApp}
 * is responsible for talking to the user.
 */
public class ParkingLot {
    public static final int DEFAULT_FLOORS = 3;
    public static final int DEFAULT_SMALL = 8;
    public static final int DEFAULT_MEDIUM = 10;
    public static final int DEFAULT_LARGE = 4;

    private final int floorCount;
    private final int smallPerFloor;
    private final int mediumPerFloor;
    private final int largePerFloor;

    private final List<Floor> floors = new ArrayList<>();
    private final Map<String, ParkingSlot> slotsById = new LinkedHashMap<>();
    private final Map<String, Ticket> activeByTicketId = new LinkedHashMap<>();
    private final Map<String, Ticket> activeByReg = new LinkedHashMap<>();
    private final List<Ticket> completedTickets = new ArrayList<>();

    private final FeeCalculator feeCalculator = new FeeCalculator();
    private int ticketSequence;

    public ParkingLot() {
        this(DEFAULT_FLOORS, DEFAULT_SMALL, DEFAULT_MEDIUM, DEFAULT_LARGE);
    }

    public ParkingLot(int floorCount, int smallPerFloor, int mediumPerFloor, int largePerFloor) {
        if (floorCount < 1 || smallPerFloor < 0 || mediumPerFloor < 0 || largePerFloor < 0) {
            throw new IllegalArgumentException("Invalid parking lot size.");
        }
        this.floorCount = floorCount;
        this.smallPerFloor = smallPerFloor;
        this.mediumPerFloor = mediumPerFloor;
        this.largePerFloor = largePerFloor;
        this.ticketSequence = 0;
        buildFloors();
    }

    private void buildFloors() {
        floors.clear();
        slotsById.clear();
        for (int f = 0; f < floorCount; f++) {
            Floor floor = new Floor(f);
            addSlots(floor, f, SlotSize.SMALL, smallPerFloor);
            addSlots(floor, f, SlotSize.MEDIUM, mediumPerFloor);
            addSlots(floor, f, SlotSize.LARGE, largePerFloor);
            floors.add(floor);
        }
    }

    private void addSlots(Floor floor, int floorNumber, SlotSize size, int count) {
        for (int i = 1; i <= count; i++) {
            String id = String.format("F%d-%s-%02d", floorNumber, size.name().charAt(0), i);
            ParkingSlot slot = new ParkingSlot(id, floorNumber, size);
            floor.addSlot(slot);
            slotsById.put(id, slot);
        }
    }

    public Ticket park(VehicleType type, String rawRegistration, LocalDateTime entryTime)
            throws InvalidInputException, VehicleAlreadyParkedException, SlotUnavailableException {
        String reg = VehicleNumberValidator.normalizeAndValidate(rawRegistration);
        if (activeByReg.containsKey(reg)) {
            Ticket existing = activeByReg.get(reg);
            throw new VehicleAlreadyParkedException(
                    "Vehicle " + reg + " is already parked (ticket " + existing.getTicketId()
                            + ", slot " + existing.getSlotId() + ").");
        }

        Vehicle vehicle = Vehicle.create(type, reg);
        ParkingSlot slot = findSlot(vehicle.getRequiredSize())
                .orElseThrow(() -> new SlotUnavailableException(
                        "No free slot available for a " + type + " right now. Please try later."));

        ticketSequence++;
        String ticketId = buildTicketId(entryTime, ticketSequence);
        slot.occupy(ticketId);

        Ticket ticket = new Ticket(ticketId, reg, type, slot.getId(), entryTime);
        activeByTicketId.put(ticketId, ticket);
        activeByReg.put(reg, ticket);
        return ticket;
    }

    public Ticket unpark(String ticketIdOrReg, LocalDateTime exitTime)
            throws InvalidInputException, InvalidTicketException {
        if (ticketIdOrReg == null || ticketIdOrReg.trim().isEmpty()) {
            throw new InvalidInputException("Please enter a ticket ID or a registration number.");
        }
        String key = ticketIdOrReg.trim().toUpperCase();
        Ticket ticket = activeByTicketId.get(key);
        if (ticket == null) {
            ticket = activeByReg.get(key.replaceAll("\\s+", ""));
        }
        if (ticket == null) {
            throw new InvalidTicketException(
                    "No active ticket found for '" + ticketIdOrReg.trim() + "'.");
        }
        if (exitTime.isBefore(ticket.getEntryTime())) {
            throw new InvalidInputException("Exit time cannot be before the entry time.");
        }

        double fee = feeCalculator.calculate(ticket.getVehicleType(), ticket.getEntryTime(), exitTime);
        ticket.complete(exitTime, fee);

        ParkingSlot slot = slotsById.get(ticket.getSlotId());
        if (slot != null) {
            slot.vacate();
        }
        activeByTicketId.remove(ticket.getTicketId());
        activeByReg.remove(ticket.getRegistrationNumber());
        completedTickets.add(ticket);
        return ticket;
    }

    public Ticket findTicket(String ticketId) throws InvalidTicketException, InvalidInputException {
        if (ticketId == null || ticketId.trim().isEmpty()) {
            throw new InvalidInputException("Please enter a ticket ID.");
        }
        String id = ticketId.trim().toUpperCase();
        Ticket active = activeByTicketId.get(id);
        if (active != null) {
            return active;
        }
        for (Ticket ticket : completedTickets) {
            if (ticket.getTicketId().equals(id)) {
                return ticket;
            }
        }
        throw new InvalidTicketException("Ticket '" + ticketId.trim() + "' was not found.");
    }

    public String availabilityReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Availability  (free / total)\n");
        int totalFree = 0;
        int totalSlots = 0;
        for (Floor floor : floors) {
            sb.append("  ").append(floor.displayName()).append('\n');
            for (SlotSize size : SlotSize.values()) {
                long free = floor.countFree(size);
                long total = floor.countTotal(size);
                totalFree += (int) free;
                totalSlots += (int) total;
                sb.append(String.format("    %-7s %2d / %2d free%n", size, free, total));
            }
        }
        sb.append(String.format("  Overall     %d / %d slots free%n", totalFree, totalSlots));
        return sb.toString();
    }

    /**
     * A simple text map of one floor, e.g. [S] free, [S*] occupied.
     */
    public String floorMap(int floorNumber) {
        if (floorNumber < 0 || floorNumber >= floors.size()) {
            return "There is no floor " + floorNumber + ".";
        }
        Floor floor = floors.get(floorNumber);
        StringBuilder sb = new StringBuilder(floor.displayName()).append('\n');
        SlotSize current = null;
        int inRow = 0;
        for (ParkingSlot slot : floor.getSlots()) {
            if (current != slot.getSize()) {
                if (current != null) {
                    sb.append('\n');
                }
                current = slot.getSize();
                inRow = 0;
            }
            if (inRow == 8) {
                sb.append('\n');
                inRow = 0;
            }
            char mark = slot.getSize().name().charAt(0);
            if (slot.isFree()) {
                sb.append("[").append(mark).append("] ");
            } else {
                sb.append("[").append(mark).append("*] ");
            }
            inRow++;
        }
        sb.append("\n  [S]/[M]/[L] = free small/medium/large   * = occupied\n");
        return sb.toString();
    }

    public String parkedVehiclesReport() {
        if (activeByTicketId.isEmpty()) {
            return "No vehicles are parked at the moment.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-16s %-8s %-14s %-10s %-20s%n",
                "Ticket", "Type", "Reg. No.", "Slot", "Entry time"));
        sb.append("-".repeat(70)).append('\n');
        for (Ticket ticket : activeByTicketId.values()) {
            sb.append(String.format("%-16s %-8s %-14s %-10s %-20s%n",
                    ticket.getTicketId(),
                    ticket.getVehicleType(),
                    ticket.getRegistrationNumber(),
                    ticket.getSlotId(),
                    ticket.formatEntryTime()));
        }
        sb.append("Total parked: ").append(activeByTicketId.size());
        return sb.toString();
    }

    /**
     * Revenue is counted when a vehicle leaves (fee is collected at exit)
     * for tickets whose exit date matches the given day.
     */
    public String dailyRevenueReport(LocalDate date) {
        double total = 0;
        int count = 0;
        Map<VehicleType, double[]> byType = new EnumMap<>(VehicleType.class);
        for (VehicleType type : VehicleType.values()) {
            byType.put(type, new double[] {0, 0}); // [count, amount]
        }
        for (Ticket ticket : completedTickets) {
            if (ticket.getStatus() != TicketStatus.COMPLETED || ticket.getExitTime() == null) {
                continue;
            }
            if (!ticket.getExitTime().toLocalDate().equals(date)) {
                continue;
            }
            count++;
            total += ticket.getFee();
            double[] row = byType.get(ticket.getVehicleType());
            row[0] += 1;
            row[1] += ticket.getFee();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Daily revenue report for ")
                .append(date.format(DateTimeFormatter.ISO_LOCAL_DATE)).append('\n');
        sb.append("-".repeat(42)).append('\n');
        for (VehicleType type : VehicleType.values()) {
            double[] row = byType.get(type);
            sb.append(String.format("  %-6s  %3.0f vehicle(s)   Rs %.2f%n", type, row[0], row[1]));
        }
        sb.append("-".repeat(42)).append('\n');
        sb.append(String.format("  TOTAL   %3d vehicle(s)   Rs %.2f%n", count, total));
        if (count == 0) {
            sb.append("  No vehicles exited on this date.\n");
        }
        return sb.toString();
    }

    public FeeCalculator getFeeCalculator() {
        return feeCalculator;
    }

    public int getFloorCount() {
        return floorCount;
    }

    public int getSmallPerFloor() {
        return smallPerFloor;
    }

    public int getMediumPerFloor() {
        return mediumPerFloor;
    }

    public int getLargePerFloor() {
        return largePerFloor;
    }

    public int getTicketSequence() {
        return ticketSequence;
    }

    public List<Ticket> getActiveTickets() {
        return Collections.unmodifiableList(new ArrayList<>(activeByTicketId.values()));
    }

    public List<Ticket> getCompletedTickets() {
        return Collections.unmodifiableList(completedTickets);
    }

    public List<Floor> getFloors() {
        return Collections.unmodifiableList(floors);
    }

    /* ---------- helpers used by DataStore to restore state ---------- */

    void restoreSequence(int sequence) {
        this.ticketSequence = sequence;
    }

    void restoreActive(Ticket ticket) {
        ParkingSlot slot = slotsById.get(ticket.getSlotId());
        if (slot != null) {
            slot.occupy(ticket.getTicketId());
        }
        activeByTicketId.put(ticket.getTicketId(), ticket);
        activeByReg.put(ticket.getRegistrationNumber(), ticket);
    }

    void restoreCompleted(Ticket ticket) {
        completedTickets.add(ticket);
    }

    private Optional<ParkingSlot> findSlot(SlotSize required) {
        for (Floor floor : floors) {
            Optional<ParkingSlot> slot = floor.findAvailable(required);
            if (slot.isPresent()) {
                return slot;
            }
        }
        return Optional.empty();
    }

    private String buildTicketId(LocalDateTime time, int sequence) {
        return String.format("TKT-%s-%04d",
                time.format(DateTimeFormatter.ofPattern("yyyyMMdd")), sequence);
    }
}
