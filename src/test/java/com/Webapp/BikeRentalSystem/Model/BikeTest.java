package com.Webapp.BikeRentalSystem.Model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BikeTest {

    @Test
    void oldSevenFieldLinesLoadWithNoOwner() {
        Bike bike = Bike.fromFileString("B001|Honda|Motorbike|Kandy|800.0|true|Active");

        assertEquals("", bike.getOwnerId());
        // and are written back with the owner field, so the file upgrades itself
        assertEquals("B001|Honda|Motorbike|Kandy|800.0|true|Active|", bike.toFileString());
    }

    @Test
    void eightFieldLinesKeepTheOwner() {
        Bike bike = Bike.fromFileString("B002|Yamaha|Scooter|Galle|2500.0|false|Active|U010");

        assertEquals("U010", bike.getOwnerId());
        assertEquals("B002|Yamaha|Scooter|Galle|2500.0|false|Active|U010", bike.toFileString());
    }

    @Test
    void wrongFieldCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Bike.fromFileString("B001|x|y|z|1.0|true"));
        assertThrows(IllegalArgumentException.class, () -> Bike.fromFileString("B001|a|b|c|1.0|true|Active|U1|extra"));
    }
}
