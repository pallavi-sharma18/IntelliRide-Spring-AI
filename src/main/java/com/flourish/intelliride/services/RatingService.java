package com.flourish.intelliride.services;

import com.flourish.intelliride.dtos.DriverDto;
import com.flourish.intelliride.dtos.RiderDto;
import com.flourish.intelliride.entities.Ride;

public interface RatingService {

    DriverDto rateDriver(Ride ride , Integer rating);
    RiderDto rateRider(Ride ride, Integer rating);

    void createNewRating(Ride ride);
}
