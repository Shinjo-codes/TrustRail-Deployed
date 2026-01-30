package trustrail.api.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trustrail.api.entity.Customer;
import trustrail.api.entity.Mandate;
import trustrail.api.entity.PaymentTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ==================== PAYMENT TRANSACTION REPOSITORY ====================
@Repository
public interface PaymentTransactionRepo extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionReference(String transactionReference);

    Optional<PaymentTransaction> findByPwaTransactionId(String pwaTransactionId);

    List<PaymentTransaction> findByMandate(Mandate mandate);

    List<PaymentTransaction> findByCustomer(Customer customer);

    List<PaymentTransaction> findByMandateOrderByCreatedAtDesc(Mandate mandate);

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.mandate = :mandate " +
            "AND pt.status = 'FAILED' ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findFailedTransactionsByMandate(@Param("mandate") Mandate mandate);

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.status = 'SCHEDULED' " +
            "AND pt.scheduledDate <= :date")
    List<PaymentTransaction> findScheduledTransactionsDue(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(pt) FROM PaymentTransaction pt WHERE pt.customer = :customer " +
            "AND pt.status = 'SUCCESSFUL'")
    long countSuccessfulByCustomer(@Param("customer") Customer customer);

    @Query("SELECT COUNT(pt) FROM PaymentTransaction pt WHERE pt.customer = :customer " +
            "AND pt.status = 'FAILED'")
    long countFailedByCustomer(@Param("customer") Customer customer);
}