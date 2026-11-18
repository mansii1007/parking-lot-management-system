# Parking Lot Management System

A menu-driven Java console application that manages a multi-floor parking lot. Vehicles are parked and unparked with tickets, hourly fees are calculated by vehicle type, and all data is saved to a file so it is still there after you close the program.

Built as a student project by **Mansi Harshal Patil**.

## Features

- Object-oriented design: abstract `Vehicle` with `Bike`, `Car` and `Truck` subclasses, plus `ParkingSlot`, `Floor`, `ParkingLot` and `Ticket`
- Three floors (Ground, 1 and 2) with small, medium and large slots
- Smart slot allocation — a bike can use a larger bay if no small slot is free, but exact-size slots are preferred so trucks keep the large bays
- Park and unpark by ticket ID or registration number
- Unique ticket IDs (`TKT-YYYYMMDD-0001`) with entry and exit timestamps
- Hourly charges, rounded up to the next hour (minimum 1 hour): Bike ₹20, Car ₹40, Truck ₹80
- Availability summary and a simple floor map (`[S]` free, `[S*]` occupied)
- Daily revenue report with a per-vehicle-type breakdown
- Data saved to a text file after every action, so parked vehicles survive a restart
- Input validation for menu choices, dates and registration numbers (e.g. `MH12AB1234`)
- Custom exceptions: `InvalidInputException`, `SlotUnavailableException`, `InvalidTicketException`, `VehicleAlreadyParkedException`

## Tech stack

Java 11+ (compiled and tested on Java 21) · console / standard I/O · file-based persistence · no external libraries

## How to run

You need a JDK (Java 11 or later). On Windows, use Command Prompt or PowerShell in this folder. On macOS or Linux, use a terminal.

**Compile**

```bash
# macOS / Linux
chmod +x compile.sh run.sh
./compile.sh

# Windows (Command Prompt)
javac -encoding UTF-8 -d out src/com/mansi/parkinglot/*.java src/com/mansi/parkinglot/model/*.java src/com/mansi/parkinglot/exception/*.java src/com/mansi/parkinglot/service/*.java src/com/mansi/parkinglot/util/*.java src/com/mansi/parkinglot/test/*.java
```

**Start the program**

```bash
# macOS / Linux
./run.sh

# Windows
java -cp out com.mansi.parkinglot.ParkingLotApp
```

The default save file is `data/parking-lot.dat`. Optional arguments:

```bash
java -cp out com.mansi.parkinglot.ParkingLotApp --data data/parking-lot.dat
java -cp out com.mansi.parkinglot.ParkingLotApp --data data/demo.dat --config 2,4,4,2
```

`--config floors,smallPerFloor,mediumPerFloor,largePerFloor` is used only when the data file does not already exist.

**Menu**

1. Park a vehicle
2. Unpark a vehicle
3. View availability
4. View ticket details
5. View parked vehicles
6. Daily revenue report
7. Save and exit

**Run the built-in tests**

```bash
java -cp out com.mansi.parkinglot.test.ParkingLotTest
```

A scripted end-to-end session (park, reject bad input, exit, restart, confirm data was saved) is in `scripts/scripted_session.sh`:

```bash
./compile.sh
bash scripts/scripted_session.sh
```

## Project structure

```
parking-lot-management-system/
├── README.md
├── .gitignore
├── compile.sh
├── run.sh
├── data/                          # runtime save files (created automatically)
├── scripts/scripted_session.sh
└── src/com/mansi/parkinglot/
    ├── ParkingLotApp.java         # main menu
    ├── model/
    │   ├── Vehicle.java           # abstract base class
    │   ├── Bike.java, Car.java, Truck.java
    │   ├── VehicleType.java, SlotSize.java
    │   ├── ParkingSlot.java, Floor.java
    │   ├── Ticket.java, TicketStatus.java
    ├── exception/                 # custom checked exceptions
    ├── service/
    │   ├── ParkingLot.java        # park / unpark / reports
    │   ├── FeeCalculator.java     # hourly billing
    │   └── DataStore.java         # load and save the text data file
    ├── util/VehicleNumberValidator.java
    └── test/ParkingLotTest.java
```

## Billing example

A car that entered at 10:00 and left at 11:10 stayed 70 minutes. That is rounded up to 2 hours, so the fee is 2 × ₹40 = ₹80. Revenue is recorded on exit.

## Sample registration numbers you can try

`MH12AB1234` · `DL01CD5678` · `KA03XY9999`
