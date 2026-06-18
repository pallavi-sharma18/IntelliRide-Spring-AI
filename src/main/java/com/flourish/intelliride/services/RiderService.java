package com.flourish.intelliride.services;

import com.flourish.intelliride.dtos.DriverDto;
import com.flourish.intelliride.dtos.RideDto;
import com.flourish.intelliride.dtos.RideRequestDto;
import com.flourish.intelliride.dtos.RiderDto;
import com.flourish.intelliride.entities.Rider;
import com.flourish.intelliride.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public interface RiderService {

    RideRequestDto requestRide(RideRequestDto rideRequestDto);
    RideDto cancelRide(Long rideId);

    DriverDto rateDriver(Long rideId, Integer rating);

    RiderDto getMyProfile();

    Page<RideDto> getAllMyRides(PageRequest pageRequest);

    Rider createNewRider(User user);

    Rider getCurrentRider();
}
