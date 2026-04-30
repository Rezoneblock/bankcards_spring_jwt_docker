package com.gordeev.bankcards.entity;

import com.gordeev.bankcards.enums.BlockRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "card_block_requests")
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CardBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String reason;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlockRequestStatus status;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "processed_by")
    private UUID processedBy;
}
