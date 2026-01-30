package trustrail.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trustrail.api.dto.AuthResponse;
import trustrail.api.dto.LoginRequest;
import trustrail.api.dto.RegisterRequest;
import trustrail.api.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Admin-only endpoint to verify business
    @PatchMapping("/verify/{businessId}")
    public ResponseEntity<AuthResponse> verifyBusiness(@PathVariable Long businessId) {
        return ResponseEntity.ok(authService.verifyBusiness(businessId));
    }
}
