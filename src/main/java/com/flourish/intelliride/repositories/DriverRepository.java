package com.flourish.intelliride.repositories;

import com.flourish.intelliride.entities.Driver;
import com.flourish.intelliride.entities.User;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver,Long> {

    @Query(value = "SELECT d.* FROM driver AS d " +
                   "WHERE d.available = true AND ST_DWithin(d.current_location, :pickupLocation, 10000) " +
                   "ORDER BY ST_Distance(d.current_location, :pickupLocation) " +
                   "LIMIT 10",
           nativeQuery = true)
    List<Driver> findTenNearestDrivers(Point pickupLocation);


    @Query(value = "SELECT d.* FROM driver AS d " +
            "WHERE d.available = true AND ST_DWithin(d.current_location, :pickupLocation, 15000) " +
            "ORDER BY d.rating DESC " +
            "LIMIT 10",
    nativeQuery = true)
    List<Driver> findTenNearbyTopRatedDrivers(Point pickupLocation);

    Optional<Driver> findByUser(User user);
}
