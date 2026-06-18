package com.flourish.intelliride.strategies;

import com.flourish.intelliride.entities.Driver;
import com.flourish.intelliride.entities.RideRequest;

import java.util.List;

public interface DriverMatchingStrategy {

    List<Driver> findMatchingDriver(RideRequest rideRequest);
}
