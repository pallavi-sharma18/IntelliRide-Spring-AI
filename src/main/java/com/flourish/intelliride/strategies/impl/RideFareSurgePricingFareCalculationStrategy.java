package com.flourish.intelliride.strategies.impl;

import com.flourish.intelliride.entities.RideRequest;
import com.flourish.intelliride.services.DistanceService;
import com.flourish.intelliride.strategies.RideFareCalculationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RideFareSurgePricingFareCalculationStrategy implements RideFareCalculationStrategy {
    private final DistanceService distanceService;
    private static final double SURGE_FACTOR = 2;
        @Override
        public double calculateFare(RideRequest rideRequest) {
            double distance = distanceService.calculateDistance(rideRequest.getPickupLocation(),
                    rideRequest.getDropOffLocation());

            return distance*RIDE_FARE_MULTIPLIER * SURGE_FACTOR;

        }
    }

