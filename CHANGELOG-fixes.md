# RIDEX – fixes made from the code review

Not run against a real Spring Boot build (Maven Central was not reachable where these fixes were made).
Checked by compiling every Java file against stand-in Spring classes and running 29 unit tests
(`src/test/java`). Please run `mvnw spring-boot:run` once and click through the pages.

## Start-up
- Start the app from the project folder (the one containing `pom.xml`).
  The data folder is `data.dir` in `application.properties` (default `src/main/resources/data`).
  To use another folder: `mvnw spring-boot:run -Dspring-boot.run.arguments=--data.dir=C:/full/path/data`
- A WARNING is printed at start-up if the data folder is missing.
- New property `payment.file.path`; new empty `data/payments.txt`.

## Whole project
- **Login/role checks** (`Config/AccessInterceptor`): every page except home, login, register, logout and
  the bike list needs a login. Admin-only: add/edit/delete bike, ride requests, complete booking,
  delete ride, edit/delete payment. Customer-only: create booking. Wrong role -> 403.
- **Booking IDs/names bug**: bookings, ride requests and payments now store real IDs (`U002`, `B001`);
  names are looked up only when a page is shown.
- **Damaged lines**: a bad line in any `.txt` file is skipped, printed to the console, and kept in the
  file when it is rewritten. Its ID number is never reused.
- `|` and line breaks typed into any form are cleaned so they cannot break a data line.
- **Validation** (`@Valid`): register, login, ride, review, booking.
- `Ride.diverId` renamed `driverId`. `TestBike.java` deleted. Template file names fixed to match
  controller view names (`home.html`, `addBike.html`, `editBike.html`, `editRide.html`).

## By module
- **Users**: Owner and Driver can be chosen at registration (Admin cannot); the last Admin cannot be deleted; deleting a
  user also removes their bookings, ride requests, rides and reviews; the password is no longer kept in the session.
- **Bikes**: an **Owner** can add bikes and edit/delete only the bikes they listed (Admin manages all).
  `bikes.txt` has an optional 8th field, the owner's user ID; old 7-field lines still load (no owner = Admin only).
  The owner is set from the login, never from the form, and an edit never changes it. New ID is assigned by the service; a bike with active bookings cannot be deleted; sorting
  (quick sort) is done in the service; the Book button shows for any Active bike.
- **Bookings**: the New Booking form picks the bike from a drop-down of Active bikes; status is always Pending when created; bike must exist and be Active; no past dates;
  no double booking of one bike on one day; only the owner or Admin can edit/delete; Admin has a Complete button.
- **Ride requests**: FIFO queue is rebuilt from the file; only the request at the front can be approved or
  rejected; one open request per booking; approve takes a seat and confirms the booking, reject cancels it.
- **Rides**: a **Driver** publishes rides (the driver is the logged-in user, the bike is picked from a list) and
  can edit/cancel only their own. Every ride has exactly **one passenger seat** (no seats field on the forms); it
  becomes Full when a request is approved. A **Customer** cannot create rides: the Rides page lists rides on the
  bikes they booked with a **Join** button, which sends a ride request for their own booking (My Requests shows
  its status). Admin sees all rides and can delete. Start and destination must differ, no past dates.
- **Payments**: read from `payments.txt`; customers only see/pay their own booking; start date cannot be in
  the past; a booking can have only one payment (cash is saved as Pending until the Admin marks it Paid);
  payment IDs use highest number + 1 so deleted IDs are not repeated.
- **Reviews**: only for a Completed booking, one review per customer per bike; Admin can delete.

## Sample data
- New sample Owner `owner1@gmail.com` (U006, password `owner123`) who owns bike B002 (`bikes.txt` 8th field).
- `rideRequests.txt` now uses real IDs and a seventh (ride) field; `bikes.txt` B001 is unavailable
  because it has a Confirmed booking.

## Not done
- Package names are still capitalised (`Controller`, `Service`...); renaming touches every file.
- Owners do not approve ride requests (the Admin does); the plan gives that to the owner.
- A deleted Owner's bikes stay in the file and become Admin-managed.
- No Pay button on My bookings yet (payment is still reached from My Rides).
- Bookings still have a single date, not a start and end date.
