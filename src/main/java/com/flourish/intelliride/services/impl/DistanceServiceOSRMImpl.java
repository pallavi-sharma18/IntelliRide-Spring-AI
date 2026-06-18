package com.flourish.intelliride.services.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.flourish.intelliride.services.DistanceService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;


@Slf4j
@Service
public class DistanceServiceOSRMImpl implements DistanceService {

    private static final String OSRM_API_BASE_URL = "https://router.project-osrm.org/route/v1/driving/";

    private final RestClient restClient = RestClient.builder()
            .baseUrl(OSRM_API_BASE_URL)
            .build();

    @Override
    public double calculateDistance(Point src, Point dest) {
        try {
            String uri = src.getX() + "," + src.getY() + ";" + dest.getX() + "," + dest.getY();

            OSRMResponseDto responseDto = restClient
                    .get()
                    .uri(uri)
                    .header("Accept-Encoding", "identity")
                    .retrieve()
                    .body(OSRMResponseDto.class);
            if (responseDto == null || responseDto.getRoutes() == null || responseDto.getRoutes().isEmpty()) {
                throw new RuntimeException("No routes returned from OSRM for uri: " + uri);
            }
            log.info("OSRM API RESPONSE: {}", responseDto);
            return responseDto.getRoutes().get(0).getDistance() / 1000.0;
        } catch (Exception e) {
            log.error("Full error: ", e);
            throw new RuntimeException("Error getting data from OSRM " + e);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OSRMResponseDto {
        private List<OSRMRoute> routes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OSRMRoute {
        private Double distance;
    }
}