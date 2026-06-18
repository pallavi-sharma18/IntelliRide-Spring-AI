package com.flourish.intelliride.tools;

import com.flourish.intelliride.dtos.DriverDto;
import com.flourish.intelliride.dtos.RideDto;
import com.flourish.intelliride.entities.Driver;
import com.flourish.intelliride.services.DriverService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DriverTools {

    private static final Logger log = LoggerFactory.getLogger(DriverTools.class);

    private final DriverService driverService;

    // ---------------------------------------------------------------------
    // Read-only tools — safe to call freely
    // ---------------------------------------------------------------------

    @Tool(name = "driver_getMyProfile", description = "Get the current driver's profile, including rating and availability.")
    public DriverDto getMyProfile() {
        audit("getMyProfile", "READ");
        return driverService.getMyProfile();
    }

    @Tool(name = "driver_getMyRides", description = "Get the current driver's recent rides with status and fare.")
    public List<RideDto> getMyRides() {
        audit("getMyRides", "READ");
        return driverService.getAllMyRides(PageRequest.of(0, 10)).getContent();
    }

    // ---------------------------------------------------------------------
    // Low-risk action — going online/offline is trivially reversible, no gate
    // ---------------------------------------------------------------------

    @Tool(description = "Set the current driver's availability. Pass available=true to go "
            + "online (accept rides) or available=false to go offline.")
    public String setAvailability(
            @ToolParam(description = "true = online/available, false = offline") boolean available) {
        try {
            Driver driver = driverService.getCurrentDriver();
            driverService.updateDriverAvailability(driver, available);
            audit("setAvailability", "EXECUTED", available);
            return "You are now " + (available ? "online and available for rides." : "offline.");
        } catch (Exception e) {
            audit("setAvailability", "FAILED", available, e.getMessage());
            return "Could not update availability: " + e.getMessage();
        }
    }

    // ---------------------------------------------------------------------
    // Action tools — change data. confirmed=false -> preview; true -> execute
    // ---------------------------------------------------------------------

    @Tool(description = "Accept a pending ride request by its ride-request id. Call with "
            + "confirmed=false first to preview; only call again with confirmed=true after "
            + "the driver explicitly approves.")
    public String acceptRide(
            @ToolParam(description = "The id of the ride request to accept") Long rideRequestId,
            @ToolParam(description = "Set true ONLY after the driver explicitly confirmed") boolean confirmed) {

        if (!confirmed) {
            audit("acceptRide", "PREVIEW", rideRequestId);
            return "CONFIRMATION REQUIRED: accept ride request " + rideRequestId
                    + "? Ask the driver to confirm before proceeding.";
        }
        try {
            RideDto ride = driverService.acceptRide(rideRequestId);
            audit("acceptRide", "EXECUTED", rideRequestId, ride.getId());
            return "Ride request " + rideRequestId + " accepted. Ride id: " + ride.getId()
                    + ", OTP: " + ride.getOtp() + " (needed to start the ride).";
        } catch (Exception e) {
            audit("acceptRide", "FAILED", rideRequestId, e.getMessage());
            return "Could not accept ride request " + rideRequestId + ": " + e.getMessage();
        }
    }

    @Tool(description = "Start a confirmed ride. Requires the ride id and the OTP the rider "
            + "provides. Call with confirmed=false first to preview; only call again with "
            + "confirmed=true after the driver confirms.")
    public String startRide(
            @ToolParam(description = "The id of the ride to start") Long rideId,
            @ToolParam(description = "The OTP provided by the rider") String otp,
            @ToolParam(description = "Set true ONLY after the driver explicitly confirmed") boolean confirmed) {

        if (!confirmed) {
            audit("startRide", "PREVIEW", rideId);   // do not log the OTP
            return "CONFIRMATION REQUIRED: start ride " + rideId
                    + " with the rider's OTP? Ask the driver to confirm before proceeding.";
        }
        try {
            driverService.startRide(rideId, otp);
            audit("startRide", "EXECUTED", rideId);  // do not log the OTP
            return "Ride " + rideId + " has started.";
        } catch (Exception e) {
            audit("startRide", "FAILED", rideId, e.getMessage());
            return "Could not start ride " + rideId + ": " + e.getMessage();
        }
    }

    @Tool(description = "End an in-progress ride by its id (this finalizes payment). Call with "
            + "confirmed=false first to preview; only call again with confirmed=true after "
            + "the driver explicitly approves.")
    public String endRide(
            @ToolParam(description = "The id of the ride to end") Long rideId,
            @ToolParam(description = "Set true ONLY after the driver explicitly confirmed") boolean confirmed) {

        if (!confirmed) {
            audit("endRide", "PREVIEW", rideId);
            return "PREVIEW ONLY — ride " + rideId + " has NOT ended. This finalizes the fare and "
                    + "payment. If the driver agrees, call endRide again with rideId=" + rideId
                    + " and confirmed=true.";
        }
        try {
            driverService.endRide(rideId);
            audit("endRide", "EXECUTED", rideId);
            return "Ride " + rideId + " has ended and payment was processed.";
        } catch (Exception e) {
            audit("endRide", "FAILED", rideId, e.getMessage());
            return "Could not end ride " + rideId + ": " + e.getMessage();
        }
    }

    @Tool(name = "driver_cancelRide", description = "Cancel one of the current driver's rides by ride id. Call with "
            + "confirmed=false first to preview; only call again with confirmed=true after "
            + "the driver explicitly approves.")
    public String cancelRide(
            @ToolParam(description = "The id of the ride to cancel") Long rideId,
            @ToolParam(description = "Set true ONLY after the driver explicitly confirmed") boolean confirmed) {

        if (!confirmed) {
            audit("cancelRide", "PREVIEW", rideId);
            return "CONFIRMATION REQUIRED: this will cancel ride " + rideId
                    + ". Ask the driver to confirm before proceeding.";
        }
        try {
            driverService.cancelRide(rideId);
            audit("cancelRide", "EXECUTED", rideId);
            return "Ride " + rideId + " has been cancelled.";
        } catch (Exception e) {
            audit("cancelRide", "FAILED", rideId, e.getMessage());
            return "Could not cancel ride " + rideId + ": " + e.getMessage();
        }
    }

    @Tool(description = "Rate the rider of a completed ride, 1 to 5 stars. Call with "
            + "confirmed=false first to preview; only call again with confirmed=true after "
            + "the driver explicitly approves.")
    public String rateRider(
            @ToolParam(description = "The id of the completed ride") Long rideId,
            @ToolParam(description = "Rating from 1 to 5") Integer stars,
            @ToolParam(description = "Set true ONLY after the driver explicitly confirmed") boolean confirmed) {

        if (!confirmed) {
            audit("rateRider", "PREVIEW", rideId, stars);
            return "CONFIRMATION REQUIRED: rate the rider of ride " + rideId + " with "
                    + stars + " star(s)? Ask the driver to confirm before proceeding.";
        }
        try {
            driverService.rateRider(rideId, stars);
            audit("rateRider", "EXECUTED", rideId, stars);
            return "Thanks — your " + stars + "-star rating for ride " + rideId + " was recorded.";
        } catch (Exception e) {
            audit("rateRider", "FAILED", rideId, stars, e.getMessage());
            return "Could not rate ride " + rideId + ": " + e.getMessage();
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void audit(String tool, String outcome, Object... args) {
        Long userId = driverService.getCurrentDriver().getUser().getId();
        log.info("TOOL {} {} user={} args={}", tool, outcome, userId, Arrays.toString(args));
    }
}