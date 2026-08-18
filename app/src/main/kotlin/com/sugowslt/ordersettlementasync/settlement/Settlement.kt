package com.sugowslt.ordersettlementasync.settlement

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "settlements",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_settlements_event_id", columnNames = ["event_id"]),
        UniqueConstraint(name = "uk_settlements_idempotency_key", columnNames = ["idempotency_key"]),
        UniqueConstraint(name = "uk_settlements_order_id", columnNames = ["order_id"]),
    ],
)
class Settlement(
    @Column(name = "event_id", nullable = false, updatable = false, length = 100)
    val eventId: String,

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 100)
    val idempotencyKey: String,

    @Column(name = "order_id", nullable = false, updatable = false)
    val orderId: Long,

    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: Long,

    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    val amount: BigDecimal,

    @Column(nullable = false, updatable = false, length = 3)
    val currency: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    val status: SettlementStatus = SettlementStatus.COMPLETED,

    @Column(name = "trace_id", nullable = false, updatable = false, length = 100)
    val traceId: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)

enum class SettlementStatus {
    COMPLETED,
}
