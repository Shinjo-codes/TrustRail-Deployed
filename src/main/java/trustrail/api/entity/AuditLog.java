package trustrail.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import trustrail.api.entity.enums.AuditEventType;

import java.time.LocalDateTime;

/**
 * AUDIT TRAIL for compliance and debugging
 */
@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "business_id")
    private Business business;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private AuditEventType eventType;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON

    private String performedBy; // User email or "SYSTEM"

    @CreationTimestamp
    private LocalDateTime createdAt;
}

