//package trustrail.api.security;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//import trustrail.api.entity.enums.BusinessStatus;
//import trustrail.api.repo.BusinessRepo;
//
//import java.util.ArrayList;
//
//// ==================== CUSTOM USER DETAILS SERVICE ====================
//@Service
//@RequiredArgsConstructor
//public class CustomUserDetailsService implements UserDetailsService {
//
//    private final BusinessRepo businessRepository;
//
//    @Override
//    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
//        var business = businessRepository.findByEmail(email)
//                .orElseThrow(() -> new UsernameNotFoundException("Business not found"));
//
//        return org.springframework.security.core.userdetails.User.builder()
//                .username(business.getEmail())
//                .password(business.getPassword())
//                .authorities(new ArrayList<>()) // Add roles if needed
//                .accountExpired(false)
//                .accountLocked(business.getStatus() != BusinessStatus.ACTIVE)
//                .credentialsExpired(false)
//                .disabled(business.getStatus() == BusinessStatus.DEACTIVATED)
//                .build();
//    }
//}
