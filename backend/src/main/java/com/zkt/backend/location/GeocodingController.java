package com.zkt.backend.location;

import com.zkt.backend.common.ApiResponse;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/geocoding")
public class GeocodingController {
    private final GeocodingService service;
    public GeocodingController(GeocodingService service) { this.service = service; }

    @GetMapping("/search")
    ApiResponse<List<GeocodingService.Place>> search(@RequestParam @NotBlank @Size(max = 100) String address,
                                                     @RequestParam(required = false) @Size(max = 50) String city) {
        return ApiResponse.ok(service.search(address, city));
    }

    @GetMapping("/reverse")
    ApiResponse<GeocodingService.Place> reverse(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude) {
        return ApiResponse.ok(service.reverse(latitude, longitude));
    }
}
