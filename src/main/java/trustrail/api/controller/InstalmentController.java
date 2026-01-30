package trustrail.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trustrail.api.dto.CreateInstalmentRequest;
import trustrail.api.dto.InstalmentResponse;
import trustrail.api.service.InstalmentService;

@RestController
@RequestMapping("/instalments")
@RequiredArgsConstructor
@Slf4j
public class InstalmentController {

    private final InstalmentService instalmentService;

    @PostMapping
    public ResponseEntity<InstalmentResponse> createInstalmentPlan(
            @Valid @RequestBody CreateInstalmentRequest request
    ) {
        InstalmentResponse response =
                instalmentService.createInstalmentPlan(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

