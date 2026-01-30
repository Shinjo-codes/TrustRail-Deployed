package trustrail.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trustrail.api.entity.Business;
import trustrail.api.entity.enums.BusinessStatus;
import trustrail.api.repo.BusinessRepo;
import trustrail.api.security.JwtService;
import trustrail.api.dto.AuthResponse;
import trustrail.api.dto.LoginRequest;
import trustrail.api.dto.RegisterRequest;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final BusinessRepo businessRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Business business = Business.builder()
                .businessName(request.getBusinessName())
                .notificationEmail(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .rcNumber(request.getRcNumber())
                .status(BusinessStatus.PENDING_VERIFICATION)
                .build();
        businessRepository.save(business);

        return AuthResponse.builder()
                .businessId(business.getId())
                .notificationEmail(business.getNotificationEmail())
                .status(business.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Authenticate user once
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Fetch business entity after successful authentication
        Business business = businessRepository.findByNotificationEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Business not found"));

        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw new RuntimeException("Business not active");
        }

        String token = jwtService.generateToken(business.getNotificationEmail(), business.getId());

        return AuthResponse.builder()
                .token(token)
                .businessId(business.getId())
                .businessName(business.getBusinessName())
                .notificationEmail(business.getNotificationEmail())
                .status(business.getStatus())
                .build();
    }

    @Transactional
    public AuthResponse verifyBusiness(Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));

        business.setStatus(BusinessStatus.ACTIVE);
        businessRepository.save(business);

        return AuthResponse.builder()
                .businessId(business.getId())
                .notificationEmail(business.getNotificationEmail())
                .status(business.getStatus())
                .build();
    }
}
