package com.mansi.parkinglot;

import com.mansi.parkinglot.exception.ParkingLotException;
import com.mansi.parkinglot.model.Ticket;
import com.mansi.parkinglot.model.VehicleType;
import com.mansi.parkinglot.service.DataStore;
import com.mansi.parkinglot.service.ParkingLot;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Console menu for the Parking Lot Management System.
 *
 * Usage:
 *   java -cp out com.mansi.parkinglot.ParkingLotApp
 *   java -cp out com.mansi.parkinglot.ParkingLotApp --data data/lot.dat
 *   java -cp out com.mansi.parkinglot.ParkingLotApp --data data/lot.dat --config 3,8,10,4
 *
 * --config floors,smallPerFloor,mediumPerFloor,largePerFloor
 * is only used when the data file does not already exist.
 */
public class ParkingLotApp {
    private final ParkingLot lot;
    private final DataStore store;
    private final Scanner scanner;
    private final Clock clock;

    /**
     * Tiny hook so tests can freeze or shift "now" without changing production code.
     */
    public interface Clock {
        LocalDateTime now();
    }

    public ParkingLotApp(ParkingLot lot, DataStore store, Scanner scanner, Clock clock) {
        this.lot = lot;
        this.store = store;
        this.scanner = scanner;
        this.clock = clock;
    }

    public static void main(String[] args) {
        Path dataFile = Paths.get("data", "parking-lot.dat");
        int[] config = null;

        for (int i = 0; i < args.length; i++) {
            if ("--data".equals(args[i]) && i + 1 < args.length) {
                dataFile = Paths.get(args[++i]);
            } else if ("--config".equals(args[i]) && i + 1 < args.length) {
                String[] parts = args[++i].split(",");
                if (parts.length != 4) {
                    System.err.println("Usage: --config floors,small,medium,large");
                    System.exit(1);
                }
                config = new int[] {
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()),
                        Integer.parseInt(parts[3].trim())
                };
            } else if ("--help".equals(args[i]) || "-h".equals(args[i])) {
                printUsage();
                return;
            }
        }

        DataStore store = new DataStore(dataFile);
        ParkingLot lot;
        try {
            if (dataFile.toFile().exists()) {
                lot = store.load();
                System.out.println("Loaded saved data from " + dataFile.toAbsolutePath());
            } else if (config != null) {
                lot = new ParkingLot(config[0], config[1], config[2], config[3]);
            } else {
                lot = new ParkingLot();
            }
        } catch (IOException ex) {
            System.err.println("Could not load data file: " + ex.getMessage());
            System.err.println("Starting with an empty parking lot.");
            lot = (config != null)
                    ? new ParkingLot(config[0], config[1], config[2], config[3])
                    : new ParkingLot();
        }

        ParkingLotApp app = new ParkingLotApp(lot, store, new Scanner(System.in), LocalDateTime::now);
        app.run();
    }

    private static void printUsage() {
        System.out.println("Parking Lot Management System");
        System.out.println("  --data <file>                 Path of the save file (default: data/parking-lot.dat)");
        System.out.println("  --config floors,s,m,l         Lot size when no save file exists");
        System.out.println("  --help                        Show this message");
    }

    public void run() {
        System.out.println();
        System.out.println("==============================================");
        System.out.println("     PARKING LOT MANAGEMENT SYSTEM");
        System.out.println("==============================================");
        System.out.println(lot.getFeeCalculator().describeRates());
        autoSaveQuietly();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = readLine("Enter choice: ");
            System.out.println();
            try {
                switch (choice.trim()) {
                    case "1":
                        handlePark();
                        break;
                    case "2":
                        handleUnpark();
                        break;
                    case "3":
                        handleAvailability();
                        break;
                    case "4":
                        handleTicketDetails();
                        break;
                    case "5":
                        System.out.println(lot.parkedVehiclesReport());
                        break;
                    case "6":
                        handleRevenue();
                        break;
                    case "7":
                        running = false;
                        break;
                    default:
                        System.out.println("Please enter a number from 1 to 7.");
                }
            } catch (ParkingLotException ex) {
                System.out.println("Error: " + ex.getMessage());
            } catch (RuntimeException ex) {
                System.out.println("Unexpected error: " + ex.getMessage());
            }
            if (running) {
                autoSaveQuietly();
                System.out.println();
            }
        }
        saveOrWarn();
        System.out.println("Data saved. Goodbye!");
    }

    private void printMenu() {
        System.out.println("--------------- Menu ---------------");
        System.out.println("  1. Park a vehicle");
        System.out.println("  2. Unpark a vehicle");
        System.out.println("  3. View availability");
        System.out.println("  4. View ticket details");
        System.out.println("  5. View parked vehicles");
        System.out.println("  6. Daily revenue report");
        System.out.println("  7. Save and exit");
        System.out.println("------------------------------------");
    }

    private void handlePark() throws ParkingLotException {
        System.out.println("Vehicle type:  1. Bike   2. Car   3. Truck");
        VehicleType type = VehicleType.fromInput(readLine("Choose type: "));
        if (type == null) {
            System.out.println("Error: Please choose 1, 2 or 3 (or type Bike, Car, Truck).");
            return;
        }
        String reg = readLine("Registration number (e.g. MH12AB1234): ");
        Ticket ticket = lot.park(type, reg, clock.now());
        System.out.println("Vehicle parked successfully.");
        System.out.println(ticket.toReceipt());
    }

    private void handleUnpark() throws ParkingLotException {
        String key = readLine("Ticket ID or registration number: ");
        Ticket ticket = lot.unpark(key, clock.now());
        int hours = lot.getFeeCalculator().billableHours(ticket.getEntryTime(), ticket.getExitTime());
        System.out.println("Vehicle unparked successfully. Billable hours: " + hours);
        System.out.println(ticket.toReceipt());
    }

    private void handleAvailability() {
        System.out.println(lot.availabilityReport());
        for (int i = 0; i < lot.getFloorCount(); i++) {
            System.out.println(lot.floorMap(i));
        }
    }

    private void handleTicketDetails() throws ParkingLotException {
        String id = readLine("Ticket ID: ");
        Ticket ticket = lot.findTicket(id);
        System.out.println(ticket.toReceipt());
    }

    private void handleRevenue() {
        String raw = readLine("Date (YYYY-MM-DD) or press Enter for today: ");
        LocalDate date;
        if (raw == null || raw.trim().isEmpty()) {
            date = clock.now().toLocalDate();
        } else {
            try {
                date = LocalDate.parse(raw.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ex) {
                System.out.println("Error: Please use the date format YYYY-MM-DD.");
                return;
            }
        }
        System.out.println(lot.dailyRevenueReport(date));
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine();
    }

    private void autoSaveQuietly() {
        try {
            store.save(lot);
        } catch (IOException ignored) {
            // a failure here is reported on exit
        }
    }

    private void saveOrWarn() {
        try {
            store.save(lot);
        } catch (IOException ex) {
            System.err.println("Warning: could not save data: " + ex.getMessage());
        }
    }
}
