package com.flourish.intelliride.strategies.impl;

import com.flourish.intelliride.entities.Driver;
import com.flourish.intelliride.entities.RideRequest;
import com.flourish.intelliride.repositories.DriverRepository;
import com.flourish.intelliride.strategies.DriverMatchingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverMatchingNearestDriverStrategy implements DriverMatchingStrategy {
    private final DriverRepository driverRepository;
    @Override
    public List<Driver> findMatchingDriver(RideRequest rideRequest) {
            return driverRepository.findTenNearestDrivers(rideRequest.getPickupLocation());
    }
}
