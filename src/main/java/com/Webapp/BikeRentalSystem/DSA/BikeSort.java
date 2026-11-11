package com.Webapp.BikeRentalSystem.DSA;


import com.Webapp.BikeRentalSystem.Model.Bike;

import java.util.List;

public class BikeSort {


    public static void quickSort(List<Bike> list, int low, int high) {
        if (low < high) {
            int pivotPosition = partition(list, low, high);

            quickSort(list, low, pivotPosition - 1);
            quickSort(list, pivotPosition + 1, high);
        }
    }


    private static int partition(List<Bike> list, int low, int high) {
        int pivot = rank(list.get(high));
        int i = low - 1;

        for (int j = low; j < high; j++) {
            if (rank(list.get(j)) <= pivot) {
                i++;


                Bike temp = list.get(i);
                list.set(i, list.get(j));
                list.set(j, temp);
            }
        }


        Bike temp = list.get(i + 1);
        list.set(i + 1, list.get(high));
        list.set(high, temp);

        return i + 1;
    }


    private static int rank(Bike bike) {
        if (bike.isAvailable()) {
            return 0;
        } else {
            return 1;
        }
    }
}