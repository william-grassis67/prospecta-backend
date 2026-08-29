package com.leadly.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadly.demo.dto.LocationResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class NominatimService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public NominatimService(
            RestClient.Builder builder,
            ObjectMapper objectMapper
    ) {
        this.restClient = builder
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader(
                        "User-Agent",
                        "Leadly/1.0"
                )
                .build();

        this.objectMapper = objectMapper;
    }

    public LocationResponse buscarLocalizacao(
            String localizacao
    ) {

        try {

            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", localizacao)
                            .queryParam("format", "json")
                            .queryParam("limit", 1)
                            .build()
                    )
                    .retrieve()
                    .body(String.class);

            JsonNode results =
                    objectMapper.readTree(response);

            if (!results.isArray() ||
                    results.isEmpty()) {

                throw new RuntimeException(
                        "Localização não encontrada: "
                                + localizacao
                );
            }

            JsonNode location = results.get(0);

            double latitude =
                    location.get("lat").asDouble();

            double longitude =
                    location.get("lon").asDouble();

            String displayName =
                    location.get("display_name").asText();

            return new LocationResponse(
                    latitude,
                    longitude,
                    displayName
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro ao buscar localização: "
                            + localizacao,
                    e
            );
        }
    }
}