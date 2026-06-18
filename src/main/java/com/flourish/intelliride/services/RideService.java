package com.flourish.intelliride.services;

import com.flourish.intelliride.entities.Driver;
import com.flourish.intelliride.entities.Ride;
import com.flourish.intelliride.entities.RideRequest;
import com.flourish.intelliride.entities.Rider;
import com.flourish.intelliride.entities.enums.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public interface RideService {
// interface for internal services not external .
    // keeping return type as entity only not DTO.
    Ride getRideById(Long rideId);
    Ride createNewRide(RideRequest rideRequest, Driver driver);

    Ride updateRideStatus(Ride ride, RideStatus rideStatus);

    Page<Ride> getAllRidesOfRider(Rider rider, PageRequest pageRequest);

    Page<Ride> getAllRidesOfDriver(Driver driver, PageRequest pageRequest);
}
