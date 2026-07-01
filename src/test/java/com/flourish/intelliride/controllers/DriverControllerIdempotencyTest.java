package com.flourish.intelliride.controllers;

import com.flourish.intelliride.dtos.RideDto;
import com.flourish.intelliride.services.DriverService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Deterministic proof of the concurrent-loser idempotency fix (follow-up #3):
 * when {@code endRide} throws a concurrency exception because another call already
 * settled the ride, the controller returns the already-settled ride as a clean 200
 * instead of letting the exception surface as a 500.
 */
class DriverControllerIdempotencyTest {

    private final DriverService driverService = mock(DriverService.class);
    private final DriverController controller = new DriverController(driverService);

    @Test
    void endRide_loserHitsUniqueConstraint_returnsSettledRideWith200() {
        RideDto settled = new RideDto();
        when(driverService.endRide(42L)).thenThrow(
                new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"wallet_transaction_transaction_id_key\""));
        when(driverService.getEndedRide(42L)).thenReturn(settled);

        ResponseEntity<RideDto> resp = controller.endRide(42L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(settled);
        verify(driverService).getEndedRide(42L);
    }

    @Test
    void endRide_loserHitsOptimisticLock_returnsSettledRideWith200() {
        RideDto settled = new RideDto();
        when(driverService.endRide(7L)).thenThrow(
                new OptimisticLockingFailureException("row was updated or deleted by another transaction"));
        when(driverService.getEndedRide(7L)).thenReturn(settled);

        ResponseEntity<RideDto> resp = controller.endRide(7L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(settled);
        verify(driverService).getEndedRide(7L);
    }

    @Test
    void endRide_happyPath_returnsResultDirectlyWithoutFallback() {
        RideDto dto = new RideDto();
        when(driverService.endRide(1L)).thenReturn(dto);

        ResponseEntity<RideDto> resp = controller.endRide(1L);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isSameAs(dto);
        verify(driverService, never()).getEndedRide(anyLong());
    }
}
