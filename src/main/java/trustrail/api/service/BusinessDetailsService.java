package trustrail.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import trustrail.api.entity.Business;
import trustrail.api.repo.BusinessRepo;

@Service
@Primary
@RequiredArgsConstructor
public class BusinessDetailsService implements UserDetailsService {

    private final BusinessRepo businessRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Business business = businessRepository.findByNotificationEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Business not found with email: " + email)
                );
      return business;

} }

