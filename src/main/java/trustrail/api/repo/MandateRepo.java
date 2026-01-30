package trustrail.api.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trustrail.api.entity.*;
import trustrail.api.entity.enums.MandateStatus;
import trustrail.api.entity.enums.MandateType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MANDATE REPOSITORY
 *
 * Handles all database operations for mandates
 * Uses type-safe enum comparisons in queries
 */
@Repository
public interface MandateRepo extends JpaRepository<Mandate, Long> {

    /**
     * Find mandate by TrustRail-generated reference
     */
    Optional<Mandate> findByMandateReference(String mandateReference);

    /**
     * Find mandate by PWA mandate ID
     */
    Optional<Mandate> findByPwaMandateId(String pwaMandateId);

    /**
     * Find all mandates for a customer
     */
    List<Mandate> findByCustomer(Customer customer);

    /**
     * Find all mandates for a business
     */
    List<Mandate> findByBusiness(Business business);

    /**
     * Find mandates by business and status
     */
    List<Mandate> findByBusinessAndStatus(Business business, MandateStatus status);

    /**
     * Find mandates due for debit
     * Uses ACTIVE status with enum-safe comparison
     *
     * @param date The date to check against nextDebitDate
     * @return Mandates that are ACTIVE and due for collection
     */
    @Query("SELECT m FROM Mandate m WHERE m.status = trustrail.api.entity.enums.MandateStatus.ACTIVE " +
            "AND m.nextDebitDate IS NOT NULL " +
            "AND m.nextDebitDate <= :date")
    List<Mandate> findDueForDebit(@Param("date") LocalDateTime date);

    /**
     * Find active mandates for a customer
     * Includes both ACTIVE and PENDING status
     *
     * @param customer The customer
     * @return Mandates in ACTIVE or PENDING status
     */
    @Query("SELECT m FROM Mandate m WHERE m.customer = :customer " +
            "AND (m.status = trustrail.api.entity.enums.MandateStatus.ACTIVE " +
            "OR m.status = trustrail.api.entity.enums.MandateStatus.PENDING)")
    List<Mandate> findActiveByCustomer(@Param("customer") Customer customer);

    /**
     * Count active mandates for a customer by type
     *
     * @param customer The customer
     * @param status The mandate status (typically MandateStatus.ACTIVE)
     * @param type The mandate type (INSTALMENT, SUBSCRIPTION, etc.)
     * @return Count of matching mandates
     */
    @Query("SELECT COUNT(m) FROM Mandate m WHERE m.customer = :customer " +
            "AND m.status = :status AND m.mandateType = :type")
    long countActiveByCustomerAndType(
            @Param("customer") Customer customer,
            @Param("status") MandateStatus status,
            @Param("type") MandateType type
    );
}