package com.zkt.backend.location;

import com.zkt.backend.common.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeocodingServiceTest {
    @Test void missingKeyReturnsConfigurationError() {
        RestClient client = RestClient.builder().requestFactory(new SimpleClientHttpRequestFactory()).build();
        GeocodingService service = new GeocodingService(client, "");
        assertThatThrownBy(() -> service.search("北京站", null))
                .isInstanceOfSatisfying(DomainException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("GEOCODING_NOT_CONFIGURED");
                    assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test void providerFailureReturnsUnavailableWithoutLeakingRequestData() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeocodingService service = new GeocodingService(builder.build(), "test-key");
        server.expect(requestTo("https://restapi.amap.com/v3/geocode/regeo?key=test-key&location=116.4,39.9"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> service.reverse(new BigDecimal("39.9"), new BigDecimal("116.4")))
                .isInstanceOfSatisfying(DomainException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("GEOCODING_UNAVAILABLE");
                    assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
        server.verify();
    }

    @Test void providerQuotaErrorReturnsUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeocodingService service = new GeocodingService(builder.build(), "test-key");
        server.expect(requestTo("https://restapi.amap.com/v3/geocode/regeo?key=test-key&location=116.5,39.8"))
                .andRespond(withSuccess("{\"status\":\"0\",\"infocode\":\"10003\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.reverse(new BigDecimal("39.8"), new BigDecimal("116.5")))
                .isInstanceOfSatisfying(DomainException.class, error ->
                        assertThat(error.getCode()).isEqualTo("GEOCODING_UNAVAILABLE"));
        server.verify();
    }
}
