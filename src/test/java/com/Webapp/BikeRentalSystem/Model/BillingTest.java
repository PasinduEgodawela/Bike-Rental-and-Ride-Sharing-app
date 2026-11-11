package com.Webapp.BikeRentalSystem.Model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BillingTest {

    private Billing billing() {
        return new Billing("BID001", "U002", "B001", 800.0, LocalDate.now(), "customer1", "c1@x.com", "0711234567");
    }

    @Test
    void totalIsDaysTimesPrice() throws Exception {
        LocalDate today = LocalDate.now();

        assertEquals(1, billing().calculateRentalDays(today, today));
        assertEquals(3, billing().calculateRentalDays(today, today.plusDays(2)));
        assertEquals(2400.0, billing().calculateTotal(today, today.plusDays(2)), 0.001);
    }

    @Test
    void pastStartAndBackwardsDatesAreRejected() {
        LocalDate today = LocalDate.now();

        assertThrows(PaymentException.class, () -> billing().calculateRentalDays(today.minusDays(1), today));
        assertThrows(PaymentException.class, () -> billing().calculateRentalDays(today.plusDays(2), today.plusDays(1)));
        assertThrows(PaymentException.class, () -> billing().calculateRentalDays(null, today));
    }

    @Test
    void suggestedStartIsNeverInThePast() {
        Billing old = new Billing("BID001", "U002", "B001", 800.0, LocalDate.now().minusDays(5), "a", "b", "c");

        assertEquals(LocalDate.now(), old.getSuggestedStart());
        assertEquals(LocalDate.now().plusDays(3),
                new Billing("BID001", "U002", "B001", 800.0, LocalDate.now().plusDays(3), "a", "b", "c").getSuggestedStart());
    }

    @Test
    void paymentIdsFollowTheHighestNumber() {
        java.util.List<Payment> payments = new java.util.ArrayList<>();
        assertEquals("P001", Payment.createId(payments));

        payments.add(new Payment("P001", "B1", "U1", 1, LocalDate.now(), "Paid", "Cash"));
        payments.add(new Payment("P002", "B2", "U1", 1, LocalDate.now(), "Paid", "Cash"));
        payments.remove(0);                                  // P001 is deleted

        // counting records would give P002 again, which is already used
        assertEquals("P003", Payment.createId(payments));
    }
}
