package trustrail.api.repo;

import trustrail.api.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trustrail.api.entity.enums.AuditEventType;

import java.util.List;

@Repository
public interface AuditLogRepo extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByCustomerOrderByCreatedAtDesc(Customer customer);
    List<AuditLog> findByBusinessOrderByCreatedAtDesc(Business business);
    List<AuditLog> findByEventTypeAndCustomer(AuditEventType eventType, Customer customer);

    @Query("SELECT a FROM AuditLog a WHERE a.customer = :customer " +
            "AND a.eventType = 'TRUST_SCORE_CHANGED' ORDER BY a.createdAt DESC")
    List<AuditLog> findTrustScoreHistory(@Param("customer") Customer customer);
}
