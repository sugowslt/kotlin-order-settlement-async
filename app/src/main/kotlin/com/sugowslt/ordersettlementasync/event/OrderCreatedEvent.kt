package com.sugowslt.ordersettlementasync.event

import java.math.BigDecimal

data class OrderCreatedEvent(
	val eventId: String,
	val eventType: String = "OrderCreated",
	val occurredAt: String,
	val traceId: String,
	val orderId: Long,
	val userId: Long,
	val amount: BigDecimal,
	val currency: String,
	val idempotencyKey: String,
	val forceFail: Boolean = false,
)
