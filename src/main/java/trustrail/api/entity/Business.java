package trustrail.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import trustrail.api.entity.enums.BusinessStatus;
import trustrail.api.entity.enums.BusinessType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

// ==================== BUSINESS ENTITY ====================
@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business extends Base implements UserDetails {

    @Column(nullable = false, unique = true)
    private String businessName;

    @Column(nullable = false, unique = true)
    private String notificationEmail;

    @Column(nullable = false)
    private String password; // Hashed

    @Column(nullable = false)
    @Size(max = 15, message = "Phone number cannot exceed 15 characters")
    private String phoneNumber;

    //@Column(nullable = false)
    private String pwaBusinessId; // PWA Business Identifier

    // Settlement details
    private String settlementAccountNumber;
    private String settlementBankCode;
    private String settlementAccountName;

    @Enumerated(EnumType.STRING)
    //@Column(nullable = false)
    private BusinessStatus status = BusinessStatus.PENDING_VERIFICATION;

    @Enumerated(EnumType.STRING)
    private BusinessType businessType;

    // Payment Policy Configuration (JSON stored as text)
    //@Column(columnDefinition = "TEXT")
    private String paymentPolicyJson;

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL)
    private Set<Customer> customers = new HashSet<>();

    @OneToMany(mappedBy = "business", cascade = CascadeType.ALL)
    private Set<Mandate> mandates = new HashSet<>();

    private String notificationPhoneNumber;
    @Column(nullable = false, unique = true)
    private String rcNumber;
    @Column(nullable = false, unique = true)
    private String tin;
    @Column(nullable = false, unique = true)
    private String address;

    private String businessShortName;

    private String whatsappContactName;

    private String whatsappContactNumber;

    //@Column(nullable = false, unique = true)
    private String billerCode;

    // ==================== USERDETAILS IMPLEMENTATION ====================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // You can customize roles later if you add a role system
        // For now, all businesses get a default "ROLE_BUSINESS"
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_BUSINESS"));
        return authorities;
    }

    @Override
    public String getUsername() {
        return this.notificationEmail;
    }

    @Override
    public boolean isAccountNonExpired() {
        // Always true for now; can add expiration logic later
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Locked if business is DEACTIVATED
        return this.status != BusinessStatus.DEACTIVATED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // No credential expiration for now
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Only ACTIVE businesses are considered enabled
        return this.status == BusinessStatus.ACTIVE;
    }
}
