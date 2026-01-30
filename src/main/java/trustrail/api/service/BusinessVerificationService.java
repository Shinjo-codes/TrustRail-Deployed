package trustrail.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trustrail.api.entity.Business;
import trustrail.api.entity.enums.BusinessStatus;
import trustrail.api.repo.BusinessRepo;

@Service
@Slf4j
@RequiredArgsConstructor
public class BusinessVerificationService {

    private final BusinessRepo businessRepository;

    @Transactional
    public void verifyBusiness(Long businessId, String verifiedBy) {
        log.info("Verifying business ID={} by {}", businessId, verifiedBy);

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Business not found"));

        if (business.getStatus() == BusinessStatus.ACTIVE) {
            log.warn("Business ID={} is already active", businessId);
            return;
        }

        business.setStatus(BusinessStatus.ACTIVE);
        businessRepository.save(business);

        log.info("Business ID={} verified successfully", businessId);
    }
}

