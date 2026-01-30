package trustrail.api.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import trustrail.api.entity.Business;
import trustrail.api.entity.Customer;

import java.util.List;
import java.util.Optional;

// ==================== CUSTOMER REPOSITORY ====================
@Repository
public interface CustomerRepo extends JpaRepository<Customer, Long> {

    Optional<Customer> findByBusinessAndExternalCustomerId(
            Business business,
            String externalCustomerId
    );


    Optional<Customer> findByEmail(String email);

    List<Customer> findByBusiness(Business business);

    List<Customer> findByBusinessAndAccountLinked(Business business, Boolean accountLinked);

    @Query("SELECT c FROM Customer c WHERE c.business = :business " +
            "AND c.email = :email OR c.phoneNumber = :phone")
    Optional<Customer> findByBusinessAndEmailOrPhone(
            @Param("business") Business business,
            @Param("email") String email,
            @Param("phone") String phone
    );

    boolean existsByBusinessAndExternalCustomerId(
            Business business,
            String externalCustomerId
    );
    Optional<Customer> findByPwaAccountReference(String pwaAccountReference);

    }