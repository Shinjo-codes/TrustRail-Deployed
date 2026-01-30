package trustrail.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

// ==================== CUSTOMER ENTITY ====================
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    // External reference (e.g., patient ID in CareFlow)
    @Column(nullable = false)
    private String externalCustomerId;

    // Bank account details (from PWA)
    private String linkedAccountNumber;
    private String linkedBankCode;
    private String linkedBankName;
    private String pwaAccountReference; // PWA's token for this account

    @Column(nullable = false)
    private Boolean accountLinked = false;

    @Column(columnDefinition = "TEXT")
    private String encryptedSecure;  // Pre-encrypted account details

    @Column(columnDefinition = "TEXT")
    private String encryptedBvn;// Pre-encrypted BVN

    @Column(columnDefinition = "TEXT")
    private String bvn;//

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    private TrustProfile trustProfile;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private Set<Mandate> mandates = new HashSet<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private Set<PaymentTransaction> transactions = new HashSet<>();
}

