package com.mansi.parkinglot.service;

import com.mansi.parkinglot.model.Ticket;
import com.mansi.parkinglot.model.TicketStatus;
import com.mansi.parkinglot.model.VehicleType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Saves and reloads the parking lot from a plain-text file so that
 * parked vehicles and completed tickets survive a restart.
 *
 * Each line starts with a tag:
 *   CONFIG|floors|small|medium|large
 *   SEQ|number
 *   ACTIVE|ticketId|reg|type|slotId|entryTime
 *   DONE|ticketId|reg|type|slotId|entryTime|exitTime|fee
 *
 * The file is written to a temporary sibling first, then moved into place,
 * so a crash mid-save will not leave a half-written data file.
 */
public class DataStore {
    private static final String VERSION = "PL1";

    private final Path file;

    public DataStore(Path file) {
        this.file = file;
    }

    public Path getFile() {
        return file;
    }

    public ParkingLot loadOrCreateDefault() throws IOException {
        if (file == null || !Files.exists(file)) {
            return new ParkingLot();
        }
        return load();
    }

    public ParkingLot load() throws IOException {
        int floors = ParkingLot.DEFAULT_FLOORS;
        int small = ParkingLot.DEFAULT_SMALL;
        int medium = ParkingLot.DEFAULT_MEDIUM;
        int large = ParkingLot.DEFAULT_LARGE;
        int seq = 0;

        // First pass: read the layout so the lot is built with the right size.
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if ("CONFIG".equals(parts[0]) && parts.length >= 5) {
                    floors = Integer.parseInt(parts[1]);
                    small = Integer.parseInt(parts[2]);
                    medium = Integer.parseInt(parts[3]);
                    large = Integer.parseInt(parts[4]);
                }
            }
        }

        ParkingLot lot = new ParkingLot(floors, small, medium, large);

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("VERSION")) {
                    continue;
                }
                String[] p = line.split("\\|", -1);
                switch (p[0]) {
                    case "CONFIG":
                        break;
                    case "SEQ":
                        seq = Integer.parseInt(p[1]);
                        break;
                    case "ACTIVE":
                        lot.restoreActive(parseActive(p));
                        break;
                    case "DONE":
                        lot.restoreCompleted(parseCompleted(p));
                        break;
                    default:
                        // ignore unknown tags so older/newer files stay readable
                        break;
                }
            }
        }
        lot.restoreSequence(seq);
        return lot;
    }

    public void save(ParkingLot lot) throws IOException {
        if (file == null) {
            return;
        }
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            writer.write("# Parking Lot data file (" + VERSION + ")\n");
            writer.write("VERSION|" + VERSION + "\n");
            writer.write("CONFIG|" + lot.getFloorCount() + "|" + lot.getSmallPerFloor()
                    + "|" + lot.getMediumPerFloor() + "|" + lot.getLargePerFloor() + "\n");
            writer.write("SEQ|" + lot.getTicketSequence() + "\n");
            for (Ticket ticket : lot.getActiveTickets()) {
                writer.write("ACTIVE|" + ticket.getTicketId()
                        + "|" + ticket.getRegistrationNumber()
                        + "|" + ticket.getVehicleType()
                        + "|" + ticket.getSlotId()
                        + "|" + ticket.formatEntryTime() + "\n");
            }
            for (Ticket ticket : lot.getCompletedTickets()) {
                writer.write("DONE|" + ticket.getTicketId()
                        + "|" + ticket.getRegistrationNumber()
                        + "|" + ticket.getVehicleType()
                        + "|" + ticket.getSlotId()
                        + "|" + ticket.formatEntryTime()
                        + "|" + ticket.formatExitTime()
                        + "|" + ticket.getFee() + "\n");
            }
        }
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Ticket parseActive(String[] p) {
        // ACTIVE|id|reg|type|slot|entry
        LocalDateTime entry = parseTime(p[5]);
        return new Ticket(p[1], p[2], VehicleType.valueOf(p[3]), p[4], entry);
    }

    private Ticket parseCompleted(String[] p) {
        // DONE|id|reg|type|slot|entry|exit|fee
        LocalDateTime entry = parseTime(p[5]);
        Ticket ticket = new Ticket(p[1], p[2], VehicleType.valueOf(p[3]), p[4], entry);
        LocalDateTime exit = parseTime(p[6]);
        double fee = Double.parseDouble(p[7]);
        ticket.complete(exit, fee);
        if (ticket.getStatus() != TicketStatus.COMPLETED) {
            ticket.complete(exit, fee);
        }
        return ticket;
    }

    private LocalDateTime parseTime(String text) {
        try {
            return LocalDateTime.parse(text, Ticket.TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            return LocalDateTime.parse(text);
        }
    }
}
