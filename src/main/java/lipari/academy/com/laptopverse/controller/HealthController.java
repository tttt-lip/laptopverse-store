package lipari.academy.com.laptopverse.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lipari.academy.com.laptopverse.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Healthiness")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<String>> checkHealth() {
        return ResponseEntity.ok(ApiResponse.success("LaptopVerse Api is On", "Status: Ok"));
    }
}
