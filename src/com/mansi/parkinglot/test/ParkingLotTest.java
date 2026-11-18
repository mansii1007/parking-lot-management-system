package com.mansi.parkinglot.test;

import com.mansi.parkinglot.exception.InvalidInputException;
import com.mansi.parkinglot.exception.InvalidTicketException;
import com.mansi.parkinglot.exception.SlotUnavailableException;
import com.mansi.parkinglot.exception.VehicleAlreadyParkedException;
import com.mansi.parkinglot.model.Ticket;
import com.mansi.parkinglot.model.VehicleType;
import com.mansi.parkinglot.service.DataStore;
import com.mansi.parkinglot.service.FeeCalculator;
import com.mansi.parkinglot.service.ParkingLot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Small self-contained checks for fee calculation, parking rules and file saving.
 * Run with: java -cp out com.mansi.parkinglot.test.ParkingLotTest
 */
public class ParkingLotTest {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        testFeeCalculator();
        testParkUnparkAndAllocation();
        testValidationAndExceptions();
        testPersistence();
        testRevenue();
        System.out.println();
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testFeeCalculator() {
        FeeCalculator calc = new FeeCalculator();
        LocalDateTime start = LocalDateTime.of(2026, 11, 18, 10, 0, 0);
        check("same-minute stay bills 1 hour",
                calc.billableHours(start, start) == 1);
        check("10 minutes bills 1 hour",
                calc.billableHours(start, start.plusMinutes(10)) == 1);
        check("exactly 60 minutes bills 1 hour",
                calc.billableHours(start, start.plusHours(1)) == 1);
        check("61 minutes bills 2 hours",
                calc.billableHours(start, start.plusHours(1).plusMinutes(1)) == 2);
        check("bike 2 hours = Rs 40",
                calc.calculate(VehicleType.BIKE, start, start.plusHours(2)) == 40.0);
        check("car 1 hour = Rs 40",
                calc.calculate(VehicleType.CAR, start, start.plusMinutes(15)) == 40.0);
        check("truck 3 hours = Rs 240",
                calc.calculate(VehicleType.TRUCK, start, start.plusHours(3)) == 240.0);
    }

    private static void testParkUnparkAndAllocation() throws Exception {
        ParkingLot lot = new ParkingLot(1, 1, 1, 1); // 1 of each size
        LocalDateTime t0 = LocalDateTime.of(2026, 11, 18, 9, 0);

        Ticket bike = lot.park(VehicleType.BIKE, "MH12AB1001", t0);
        check("bike gets a small slot", bike.getSlotId().contains("-S-"));

        Ticket car = lot.park(VehicleType.CAR, "MH12AB1002", t0.plusMinutes(5));
        check("car gets a medium slot", car.getSlotId().contains("-M-"));

        Ticket truck = lot.park(VehicleType.TRUCK, "MH12AB1003", t0.plusMinutes(10));
        check("truck gets a large slot", truck.getSlotId().contains("-L-"));

        boolean full = false;
        try {
            lot.park(VehicleType.BIKE, "MH12AB1999", t0.plusMinutes(15));
        } catch (SlotUnavailableException ex) {
            full = true;
        }
        check("fourth vehicle is rejected when lot is full", full);

        Ticket done = lot.unpark(bike.getTicketId(), t0.plusMinutes(90)); // 1.5 h -> 2h * 20 = 40
        check("unpark fee for 90 min bike is 40", done.getFee() == 40.0);
        check("ticket marked completed", "COMPLETED".equals(done.getStatus().name()));
    }

    private static void testValidationAndExceptions() throws Exception {
        ParkingLot lot = new ParkingLot(1, 2, 2, 1);
        LocalDateTime t0 = LocalDateTime.of(2026, 11, 18, 11, 0);

        boolean emptyRejected = false;
        try {
            lot.park(VehicleType.CAR, "   ", t0);
        } catch (InvalidInputException ex) {
            emptyRejected = true;
        }
        check("empty registration is rejected", emptyRejected);

        boolean badPlate = false;
        try {
            lot.park(VehicleType.CAR, "???", t0);
        } catch (InvalidInputException ex) {
            badPlate = true;
        }
        check("invalid registration is rejected", badPlate);

        Ticket first = lot.park(VehicleType.CAR, "mh14cd5555", t0);
        boolean duplicate = false;
        try {
            lot.park(VehicleType.CAR, "MH14CD5555", t0.plusMinutes(1));
        } catch (VehicleAlreadyParkedException ex) {
            duplicate = true;
        }
        check("duplicate vehicle is rejected", duplicate);
        check("registration is stored in uppercase",
                first.getRegistrationNumber().equals("MH14CD5555"));

        boolean badTicket = false;
        try {
            lot.findTicket("TKT-20990101-9999");
        } catch (InvalidTicketException ex) {
            badTicket = true;
        }
        check("unknown ticket is rejected", badTicket);
    }

    private static void testPersistence() throws Exception {
        Path file = Files.createTempFile("parking-lot-test", ".dat");
        try {
            DataStore store = new DataStore(file);
            ParkingLot lot = new ParkingLot(2, 2, 2, 1);
            LocalDateTime t0 = LocalDateTime.of(2026, 11, 18, 8, 0);

            Ticket stillParked = lot.park(VehicleType.CAR, "DL01AB4321", t0);
            Ticket left = lot.park(VehicleType.BIKE, "KA03XY2222", t0.plusMinutes(10));
            lot.unpark(left.getTicketId(), t0.plusHours(3)); // 3h * 20 = 60
            store.save(lot);

            ParkingLot reloaded = store.load();
            check("active ticket survives restart",
                    reloaded.findTicket(stillParked.getTicketId())
                            .getRegistrationNumber().equals("DL01AB4321"));
            check("completed ticket survives restart",
                    reloaded.findTicket(left.getTicketId()).getFee() == 60.0);
            check("occupied slot is still occupied after reload",
                    reloaded.parkedVehiclesReport().contains("DL01AB4321"));
            check("ticket sequence continues after reload",
                    reloaded.getTicketSequence() == 2);
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(file.resolveSibling(file.getFileName() + ".tmp"));
        }
    }

    private static void testRevenue() throws Exception {
        ParkingLot lot = new ParkingLot(1, 2, 2, 1);
        LocalDateTime morning = LocalDateTime.of(2026, 11, 18, 9, 0);
        Ticket car = lot.park(VehicleType.CAR, "MH01AA1111", morning);
        lot.unpark(car.getTicketId(), morning.plusHours(2)); // 80

        Ticket bike = lot.park(VehicleType.BIKE, "MH01AA2222", morning.plusHours(3));
        lot.unpark(bike.getTicketId(), morning.plusHours(4)); // 20

        String today = lot.dailyRevenueReport(LocalDate.of(2026, 11, 18));
        check("revenue report includes car amount", today.contains("80.00"));
        check("revenue report total is 100", today.contains("100.00"));
        check("other day has zero revenue",
                lot.dailyRevenueReport(LocalDate.of(2026, 11, 17)).contains("0 vehicle"));
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }
}
