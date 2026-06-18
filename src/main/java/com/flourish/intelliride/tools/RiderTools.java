package com.flourish.intelliride.tools;

import com.flourish.intelliride.dtos.PointDto;
import com.flourish.intelliride.dtos.RideDto;
import com.flourish.intelliride.dtos.RideRequestDto;
import com.flourish.intelliride.dtos.RiderDto;
import com.flourish.intelliride.entities.enums.PaymentMethod;
import com.flourish.intelliride.services.RiderService;
import com.flourish.intelliride.services.WalletService;
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
public class RiderTools {

    private static final Logger log = LoggerFactory.getLogger(RiderTools.class);

    private final RiderService riderService;
    private final WalletService walletService;

    // ---------------------------------------------------------------------
    // Read-only tools — safe to call freely, no confirmation needed
    // ---------------------------------------------------------------------

    @Tool(name = "rider_getMyProfile", description = "Get the current rider's profile, including their name and rating.")
    public RiderDto getMyProfile() {
        audit("getMyProfile", "READ");
        return riderService.getMyProfile();
    }

    @Tool(name = "rider_getMyRides", description = "Get the current rider's recent rides with status and fare.")
    public List<RideDto> getMyRides() {
        audit("getMyRides", "READ");
        return riderService.getAllMyRides(PageRequest.of(0, 10)).getContent();
    }

    @Tool(description = "Get the current rider's wallet balance.")
    public Double getWalletBalance() {
        audit("getWalletBalance", "READ");
        var user = riderService.getCurrentRider().getUser();
        return walletService.findByUser(user).getBalance();
    }

    // ---------------------------------------------------------------------
    // Action tools — change data. Two-step confirm gate:
    //   confirmed=false -> preview only; confirmed=true -> execute.
    // ---------------------------------------------------------------------

    @Tool(description = "Request a new ride for the current rider from a pickup to a drop-off "
            + "location. Latitude and longitude are decimal degrees. paymentMethod must be "
            + "CASH or WALLET. Call with confirmed=false first to show the rider the trip "
            + "details, then call again with confirmed=true only after they explicitly approve.")
    public String requestRide(
            @ToolParam(description = "Pickup latitude in decimal degrees")  double pickupLat,
            @ToolParam(description = "Pickup longitude in decimal degrees") double pickupLng,
            @ToolParam(description = "Drop-off latitude in decimal degrees")  double dropLat,
            @ToolParam(description = "Drop-off longitude in decimal degrees") double dropLng,
            @ToolParam(description = "Payment method: CASH or WALLET")      String paymentMethod,
            @ToolParam(description = "Set true ONLY after the user explicitly confirmed") boolean confirmed) {

        if (!confirmed) {
            audit("requestRide", "PREVIEW", pickupLat, pickupLng, dropLat, dropLng, paymentMethod);
            return "PREVIEW ONLY — nothing has been booked. This will book a " + paymentMethod
                    + " ride from (" + pickupLat + ", " + pickupLng + ") to (" + dropLat + ", " + dropLng
                    + "). Show this to the user. If they agree, call requestRide again with the same "
                    + "arguments and confirmed=true.";
        }
        try {
            // PointDto stores coordinates as [longitude, latitude] (x, y) — order matters!
            PointDto pickup  = new PointDto(new double[]{ pickupLng, pickupLat });
            PointDto dropOff = new PointDto(new double[]{ dropLng,   dropLat   });

            RideRequestDto dto = new RideRequestDto();
            dto.setPickupLocation(pickup);
            dto.setDropOffLocation(dropOff);
            dto.setPaymentMethod(parsePaymentMethod(paymentMethod));

            RideRequestDto result = riderService.requestRide(dto);
            audit("requestRide", "EXECUTED", result.getId(), result.getFare());
            return "Ride requested successfully. Request id: " + result.getId()
                    + ", estimated fare: " + result.getFare()
                    + ", payment: " + result.getPaymentMethod() + ".";
        } catch (Exception e) {
            audit("requestRide", "FAILED", e.getMessage());
            return "Could not request the ride: " + e.getMessage();
        }
    }

    @Tool(name = "rider_cancelRide", description = "Cancel one of the current rider's rides by ride id. Call with "
            + "confirmed=false first to preview; only call again with confirmed=true after "
            + "the user explicitly approves the cancellation.")
    public String cancelRide(
            @ToolParam(description = "The id of the ride to cancel") Long rideId,
            @ToolParam(description = "Set true ONLY after the user explicitly confirmed") boolean confirmed) {

        if (!confirmed) {
            audit("cancelRide", "PREVIEW", rideId);
            return "PREVIEW ONLY — ride " + rideId + " has NOT been cancelled. Show this to the user. "
                    + "If they agree, call cancelRide again with rideId=" + rideId + " and confirmed=true.";
        }
        try {
            riderService.cancelRide(rideId);
            audit("cancelRide", "EXECUTED", rideId);
            return "Ride " + rideId + " has been cancelled.";
        } catch (Exception e) {
            audit("cancelRide", "FAILED", rideId, e.getMessage());
            return "Could not cancel ride " + rideId + ": " + e.getMessage();
        }
    }

    @Tool(description = "Rate the driver of a completed ride, 1 to 5 stars. Call with "
            + "confirmed=false first to preview; only call again with confirmed=true after "
            + "the user explicitly approves.")
    public String rateDriver(
            @ToolParam(description = "The id of the completed ride") Long rideId,
            @ToolParam(description = "Rating from 1 to 5") Integer stars,
            @ToolParam(description = "Set true ONLY after the user explicitly confirmed") boolean confirmed) {

        if (!confirmed) {
            audit("rateDriver", "PREVIEW", rideId, stars);
            return "PREVIEW ONLY — no rating submitted yet. This will rate ride " + rideId + " with "
                    + stars + " star(s). If the user agrees, call rateDriver again with rideId=" + rideId
                    + ", stars=" + stars + " and confirmed=true.";
        }
        try {
            riderService.rateDriver(rideId, stars);
            audit("rateDriver", "EXECUTED", rideId, stars);
            return "Thanks — your " + stars + "-star rating for ride " + rideId + " was recorded.";
        } catch (Exception e) {
            audit("rateDriver", "FAILED", rideId, stars, e.getMessage());
            return "Could not rate ride " + rideId + ": " + e.getMessage();
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void audit(String tool, String outcome, Object... args) {
        Long userId = riderService.getCurrentRider().getUser().getId();
        log.info("TOOL {} {} user={} args={}", tool, outcome, userId, Arrays.toString(args));
    }

    private PaymentMethod parsePaymentMethod(String value) {
        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Invalid payment method '" + value + "'. Use CASH or WALLET.");
        }
    }
}