package trustrail.api.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trustrail.api.entity.Business;
import trustrail.api.entity.enums.BusinessStatus;

import java.util.List;
import java.util.Optional;

// ==================== BUSINESS REPOSITORY ====================
@Repository
public interface BusinessRepo extends JpaRepository<Business, Long> {

    Optional<Business> findByNotificationEmail(String email);

    Optional<Business> findByBusinessName(String businessName);

    Optional<Business> findByPwaBusinessId(String pwaBusinessId);

    boolean existsByNotificationEmail(String email);

    boolean existsByBusinessName(String businessName);

    List<Business> findByStatus(BusinessStatus status);


}
