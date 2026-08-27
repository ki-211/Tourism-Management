package com.zkt.backend.location;

import com.fasterxml.jackson.databind.JsonNode;
import com.zkt.backend.common.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GeocodingService {
    private final RestClient client;
    private final String key;

    public GeocodingService(RestClient.Builder builder, @Value("${app.amap-web-key:}") String key) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.client = builder.requestFactory(requestFactory).build();
        this.key = key;
    }

    GeocodingService(RestClient client, String key) {
        this.client = client;
        this.key = key;
    }

    public List<Place> search(String address, String city) {
        requireConfigured();
        URI uri = UriComponentsBuilder.fromUriString("https://restapi.amap.com/v3/geocode/geo")
                .queryParam("key", key).queryParam("address", address).queryParamIfPresent("city", java.util.Optional.ofNullable(city))
                .build().encode().toUri();
        JsonNode root = request(uri, "search");
        List<Place> result = new ArrayList<>();
        try {
            for (JsonNode item : root.path("geocodes")) {
                String[] point = item.path("location").asText().split(",");
                if (point.length == 2) result.add(new Place(item.path("formatted_address").asText(),
                        new BigDecimal(point[1]), new BigDecimal(point[0])));
            }
        } catch (NumberFormatException e) {
            log.warn("Amap geocoding returned malformed coordinates operation=search");
            throw DomainException.unavailable("GEOCODING_UNAVAILABLE", "地址服务返回了无效数据，请稍后重试");
        }
        return result;
    }

    public Place reverse(BigDecimal latitude, BigDecimal longitude) {
        requireConfigured();
        URI uri = UriComponentsBuilder.fromUriString("https://restapi.amap.com/v3/geocode/regeo")
                .queryParam("key", key).queryParam("location", longitude + "," + latitude).build().encode().toUri();
        JsonNode root = request(uri, "reverse");
        String address = root.path("regeocode").path("formatted_address").asText();
        if (address.isBlank()) throw DomainException.unavailable("GEOCODING_UNAVAILABLE", "暂时无法解析该位置的地址");
        return new Place(address, latitude, longitude);
    }

    private JsonNode request(URI uri, String operation) {
        long started = System.nanoTime();
        try {
            JsonNode root = client.get().uri(uri).retrieve().body(JsonNode.class);
            ensureSuccess(root, operation);
            log.debug("Amap geocoding completed operation={} elapsedMs={}", operation, elapsedMillis(started));
            return root;
        } catch (DomainException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("Amap geocoding unavailable operation={} elapsedMs={} errorType={}", operation, elapsedMillis(started), e.getClass().getSimpleName());
            throw DomainException.unavailable("GEOCODING_UNAVAILABLE", "地址服务暂时不可用，请稍后重试");
        }
    }

    private long elapsedMillis(long started) { return Duration.ofNanos(System.nanoTime() - started).toMillis(); }
    private void requireConfigured() {
        if (key == null || key.isBlank()) throw DomainException.badRequest("GEOCODING_NOT_CONFIGURED", "服务端尚未配置高德 Web Service Key");
    }
    private void ensureSuccess(JsonNode root, String operation) {
        if (root == null || !"1".equals(root.path("status").asText())) {
            String providerCode = root == null ? "EMPTY" : root.path("infocode").asText("UNKNOWN");
            log.warn("Amap geocoding rejected operation={} providerCode={}", operation, providerCode);
            throw DomainException.unavailable("GEOCODING_UNAVAILABLE", "地址服务暂时不可用，请稍后重试");
        }
    }
    public record Place(String address, BigDecimal latitude, BigDecimal longitude) {}
}
