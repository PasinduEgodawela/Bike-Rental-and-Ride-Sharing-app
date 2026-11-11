package com.Webapp.BikeRentalSystem.DSA;

import com.Webapp.BikeRentalSystem.Model.RideRequest;
import com.Webapp.BikeRentalSystem.Service.DSA.RequestQueue;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestQueueTest {

    private RideRequest request(String id) {
        return new RideRequest(id, "BID001", "U002", "B001", LocalDate.now(), "Pending", "");
    }

    @Test
    void firstInIsFirstOut() {
        RequestQueue queue = new RequestQueue();
        queue.enqueue(request("RRID001"));
        queue.enqueue(request("RRID002"));
        queue.enqueue(request("RRID003"));

        assertEquals("RRID001", queue.peek().getRequestId());
        assertEquals("RRID001", queue.dequeue().getRequestId());
        assertEquals("RRID002", queue.dequeue().getRequestId());
        assertEquals("RRID003", queue.dequeue().getRequestId());
        assertTrue(queue.isEmpty());
    }

    @Test
    void emptyQueueGivesNull() {
        RequestQueue queue = new RequestQueue();

        assertNull(queue.peek());
        assertNull(queue.dequeue());
    }
}
