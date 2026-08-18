package com.sugowslt.ordersettlementasync.settlement

import org.springframework.data.jpa.repository.JpaRepository

interface SettlementRepository : JpaRepository<Settlement, Long> {
    fun existsByEventIdOrIdempotencyKeyOrOrderId(eventId: String, idempotencyKey: String, orderId: Long): Boolean

    fun findByOrderId(orderId: Long): Settlement?
}
