package com.example.rentalservice.service;

import com.example.rentalservice.api.dto.GeocodingEstimateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GeocodingService {

    private final RestClient restClient;
    private final String apiKey;
    private final double depotLat;
    private final double depotLng;
    private final double freeDistanceKm;
    private final double surchargePerKm;

    public GeocodingService(
            @Value("${app.integrations.yandex-maps.base-url:https://geocode-maps.yandex.ru/1.x/}") String geocodingUrl,
            @Value("${app.integrations.yandex-maps.api-key:}") String apiKey,
            @Value("${app.logistics.depot.latitude:53.9023}") double depotLat,
            @Value("${app.logistics.depot.longitude:27.5619}") double depotLng,
            @Value("${app.logistics.free-distance-km:5}") double freeDistanceKm,
            @Value("${app.logistics.surcharge-per-km:2}") double surchargePerKm
    ) {
        this.restClient = RestClient.builder().baseUrl(geocodingUrl).build();
        this.apiKey = apiKey;
        this.depotLat = depotLat;
        this.depotLng = depotLng;
        this.freeDistanceKm = freeDistanceKm;
        this.surchargePerKm = surchargePerKm;
    }

    public GeocodingEstimateResponse estimateForAddress(String rawAddress) {
        String address = rawAddress == null ? "" : rawAddress.trim();
        if (address.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address must not be blank");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Yandex Geocoder API key is not configured");
        }

        JsonNode body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("apikey", apiKey)
                        .queryParam("geocode", address)
                        .queryParam("lang", "ru_RU")
                        .queryParam("format", "json")
                        .queryParam("results", 1)
                        .build())
                .retrieve()
                .body(JsonNode.class);
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address geocoding failed");
        }
        JsonNode first = body.path("response")
                .path("GeoObjectCollection")
                .path("featureMember")
                .path(0)
                .path("GeoObject");
        if (first.isMissingNode()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address not found");
        }
        String formatted = first.path("metaDataProperty")
                .path("GeocoderMetaData")
                .path("text")
                .asText(address);
        String pos = first.path("Point").path("pos").asText("");
        String[] lonLat = pos.trim().split("\\s+");
        if (lonLat.length < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordinates are unavailable");
        }
        double lng;
        double lat;
        try {
            lng = Double.parseDouble(lonLat[0]);
            lat = Double.parseDouble(lonLat[1]);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordinates are unavailable");
        }
        if (Double.isNaN(lat) || Double.isNaN(lng)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordinates are unavailable");
        }

        double distance = haversineKm(depotLat, depotLng, lat, lng);
        double paidDistance = Math.max(0, distance - freeDistanceKm);
        double surcharge = round2(paidDistance * surchargePerKm);

        return new GeocodingEstimateResponse(
                address,
                formatted,
                round6(lat),
                round6(lng),
                round2(distance),
                surcharge
        );
    }

    public GeocodingEstimateResponse estimateForPoint(double lat, double lng) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Yandex Geocoder API key is not configured");
        }
        JsonNode body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("apikey", apiKey)
                        .queryParam("geocode", lng + "," + lat)
                        .queryParam("kind", "house")
                        .queryParam("lang", "ru_RU")
                        .queryParam("format", "json")
                        .queryParam("results", 1)
                        .build())
                .retrieve()
                .body(JsonNode.class);
        JsonNode first = body == null ? null : body.path("response")
                .path("GeoObjectCollection")
                .path("featureMember")
                .path(0)
                .path("GeoObject");
        if (first == null || first.isMissingNode()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address not found for selected point");
        }
        String formatted = first.path("metaDataProperty")
                .path("GeocoderMetaData")
                .path("text")
                .asText("Selected point");
        double distance = haversineKm(depotLat, depotLng, lat, lng);
        double paidDistance = Math.max(0, distance - freeDistanceKm);
        double surcharge = round2(paidDistance * surchargePerKm);
        return new GeocodingEstimateResponse(
                formatted,
                formatted,
                round6(lat),
                round6(lng),
                round2(distance),
                surcharge
        );
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static double round6(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }
}
