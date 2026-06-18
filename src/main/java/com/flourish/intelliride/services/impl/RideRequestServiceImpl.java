package com.flourish.intelliride.services.impl;

import com.flourish.intelliride.entities.RideRequest;
import com.flourish.intelliride.exceptions.ResourceNotFoundException;
import com.flourish.intelliride.repositories.RideRequestRepository;
import com.flourish.intelliride.services.RideRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RideRequestServiceImpl implements RideRequestService {
    private final RideRequestRepository rideRequestRepository;
    @Override
    public RideRequest findRideRequestById(Long rideRequestId) {
        return rideRequestRepository.findById(rideRequestId).orElseThrow(() -> new ResourceNotFoundException("Ride Request not found with id: " + rideRequestId));
    }

    @Override
    public void update(RideRequest rideRequest) {

        rideRequestRepository.findById(rideRequest.getId()).orElseThrow(() ->
                new ResourceNotFoundException("RideRequest not found with id: " + rideRequest.getId()));

        rideRequestRepository.save(rideRequest);

    }
}
