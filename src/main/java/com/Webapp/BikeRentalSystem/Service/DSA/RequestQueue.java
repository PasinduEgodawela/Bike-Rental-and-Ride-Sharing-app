package com.Webapp.BikeRentalSystem.Service.DSA;

import com.Webapp.BikeRentalSystem.Model.RideRequest;

public class RequestQueue {

    private Node front, rear;


    public void enqueue(RideRequest rideRequest) {
        Node n = new Node(rideRequest);
        if (rear == null) { front = rear = n; return; }
        rear.next = n;
        rear = n;
    }


    public RideRequest dequeue() {
        if (front == null) return null;
        RideRequest r = front.data;
        front = front.next;
        if (front == null) rear = null;
        return r;
    }


    public RideRequest peek() {
        return front == null ? null : front.data;
    }

    public boolean isEmpty() {
        return front == null;
    }
}
