package com.Webapp.BikeRentalSystem.Service.DSA;

import com.Webapp.BikeRentalSystem.Model.RideRequest;

public class Node {

    RideRequest data;
    Node next;

    public Node(RideRequest data) {
        this.data = data;
    }
}
