package trustrail.api.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trustrail.api.entity.Business;
import trustrail.api.entity.Customer;
import trustrail.api.entity.TrustProfile;
import trustrail.api.entity.enums.TrustState;

import java.util.List;
import java.util.Optional;

// ==================== TRUST PROFILE REPOSITORY ====================
@Repository
public interface TrustProfileRepo extends JpaRepository<TrustProfile, Long> {

    Optional<TrustProfile> findByCustomer(Customer customer);

    List<TrustProfile> findByTrustState(TrustState trustState);

    @Query("SELECT tp FROM TrustProfile tp WHERE tp.customer.business = :business " +
            "AND tp.trustState = :state")
    List<TrustProfile> findByBusinessAndTrustState(
            @Param("business") Business business,
            @Param("state") TrustState state
    );

    @Query("SELECT tp FROM TrustProfile tp WHERE tp.customer.business = :business " +
            "AND tp.overallTrustScore >= :minScore")
    List<TrustProfile> findHighTrustCustomers(
            @Param("business") Business business,
            @Param("minScore") Integer minScore
    );

    @Query("SELECT tp FROM TrustProfile tp WHERE tp.customer.business = :business " +
            "AND tp.consecutiveFailures >= :threshold")
    List<TrustProfile> findAtRiskCustomers(
            @Param("business") Business business,
            @Param("threshold") Integer threshold
    );
}
