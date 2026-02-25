package com.sugowslt.ordersettlementasync.api

import com.sugowslt.ordersettlementasync.event.OrderCreatedEvent
import com.sugowslt.ordersettlementasync.kafka.OrderEventProducer
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.validation.annotation.Validated
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@RestController
@Validated
@RequestMapping("/api/v1/events")
class OrderEventController(
	private val orderEventProducer: OrderEventProducer,
) {

	@PostMapping("/orders")
	@ResponseStatus(HttpStatus.ACCEPTED)
	fun publishOrderCreated(@Valid @RequestBody request: PublishOrderEventRequest): PublishOrderEventResponse {
		val eventId = "evt-order-created-${UUID.randomUUID()}"
		val event = OrderCreatedEvent(
			eventId = eventId,
			occurredAt = Instant.now().toString(),
			traceId = request.traceId,
			orderId = request.orderId,
			userId = request.userId,
			amount = request.amount,
			currency = request.currency,
			idempotencyKey = request.idempotencyKey ?: "order-create-${request.orderId}",
			forceFail = request.forceFail,
		)

		orderEventProducer.publish(event)

		return PublishOrderEventResponse(
			eventId = event.eventId,
			orderId = event.orderId,
			traceId = event.traceId,
			status = "PUBLISHED",
		)
	}
}

data class PublishOrderEventRequest(
	@field:Positive
	val orderId: Long,
	@field:Positive
	val userId: Long,
	@field:DecimalMin(value = "0.01")
	val amount: BigDecimal,
	@field:NotBlank
	val currency: String = "KRW",
	@field:NotBlank
	val traceId: String,
	val idempotencyKey: String? = null,
	val forceFail: Boolean = false,
)

data class PublishOrderEventResponse(
	val eventId: String,
	val orderId: Long,
	val traceId: String,
	val status: String,
)
