package com.flourish.intelliride.repositories;

import com.flourish.intelliride.entities.Driver;
import com.flourish.intelliride.entities.Rating;
import com.flourish.intelliride.entities.Ride;
import com.flourish.intelliride.entities.Rider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating,Long> {
    List<Rating> findByRider(Rider rider);
    List<Rating> findByDriver(Driver driver);

    Optional<Rating> findByRide(Ride ride);
}
