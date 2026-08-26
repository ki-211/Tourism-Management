package com.zkt.backend.location;

import com.fasterxml.jackson.databind.JsonNode;
import com.zkt.backend.common.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeocodingService {
    private final RestClient client = RestClient.create();
    private final String key;

    public GeocodingService(@Value("${app.amap-web-key:}") String key) { this.key = key; }

    public List<Place> search(String address, String city) {
        requireConfigured();
        URI uri = UriComponentsBuilder.fromUriString("https://restapi.amap.com/v3/geocode/geo")
                .queryParam("key", key).queryParam("address", address).queryParamIfPresent("city", java.util.Optional.ofNullable(city))
                .build().encode().toUri();
        JsonNode root = client.get().uri(uri).retrieve().body(JsonNode.class);
        ensureSuccess(root);
        List<Place> result = new ArrayList<>();
        for (JsonNode item : root.path("geocodes")) {
            String[] point = item.path("location").asText().split(",");
            if (point.length == 2) result.add(new Place(item.path("formatted_address").asText(),
                    new BigDecimal(point[1]), new BigDecimal(point[0])));
        }
        return result;
    }

    public Place reverse(BigDecimal latitude, BigDecimal longitude) {
        requireConfigured();
        URI uri = UriComponentsBuilder.fromUriString("https://restapi.amap.com/v3/geocode/regeo")
                .queryParam("key", key).queryParam("location", longitude + "," + latitude).build().encode().toUri();
        JsonNode root = client.get().uri(uri).retrieve().body(JsonNode.class);
        ensureSuccess(root);
        return new Place(root.path("regeocode").path("formatted_address").asText(), latitude, longitude);
    }

    private void requireConfigured() {
        if (key == null || key.isBlank()) throw DomainException.badRequest("GEOCODING_NOT_CONFIGURED", "服务端尚未配置高德 Web Service Key");
    }
    private void ensureSuccess(JsonNode root) {
        if (root == null || !"1".equals(root.path("status").asText()))
            throw DomainException.badRequest("GEOCODING_FAILED", "地址解析失败，请稍后重试或手动选点");
    }
    public record Place(String address, BigDecimal latitude, BigDecimal longitude) {}
}
