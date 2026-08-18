package com.sugowslt.ordersettlementasync.settlement

import com.sugowslt.ordersettlementasync.event.OrderCreatedEvent
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal

@Service
class SettlementService(
    private val settlementRepository: SettlementRepository,
    transactionManager: PlatformTransactionManager,
) : SettlementProcessor {

    private val transactionTemplate = TransactionTemplate(transactionManager)

    override fun process(event: OrderCreatedEvent): SettlementProcessResult {
        validate(event)

        return try {
            transactionTemplate.execute {
                if (isDuplicate(event)) {
                    return@execute SettlementProcessResult.DUPLICATE
                }

                settlementRepository.saveAndFlush(
                    Settlement(
                        eventId = event.eventId,
                        idempotencyKey = event.idempotencyKey,
                        orderId = event.orderId,
                        userId = event.userId,
                        amount = event.amount,
                        currency = event.currency.uppercase(),
                        traceId = event.traceId,
                    ),
                )
                SettlementProcessResult.CREATED
            } ?: error("settlement transaction returned no result")
        } catch (exception: DataIntegrityViolationException) {
            if (isDuplicate(event)) {
                SettlementProcessResult.DUPLICATE
            } else {
                throw exception
            }
        }
    }

    @Transactional(readOnly = true)
    fun getByOrderId(orderId: Long): Settlement? = settlementRepository.findByOrderId(orderId)

    private fun isDuplicate(event: OrderCreatedEvent): Boolean =
        settlementRepository.existsByEventIdOrIdempotencyKeyOrOrderId(
            event.eventId,
            event.idempotencyKey,
            event.orderId,
        )

    private fun validate(event: OrderCreatedEvent) {
        require(event.eventId.isNotBlank()) { "eventId must not be blank" }
        require(event.eventId.length <= 100) { "eventId must be 100 characters or fewer" }
        require(event.idempotencyKey.isNotBlank()) { "idempotencyKey must not be blank" }
        require(event.idempotencyKey.length <= 100) { "idempotencyKey must be 100 characters or fewer" }
        require(event.orderId > 0) { "orderId must be positive" }
        require(event.userId > 0) { "userId must be positive" }
        require(event.amount > BigDecimal.ZERO) { "amount must be positive" }
        require(event.amount.precision() <= 19) { "amount precision must be 19 digits or fewer" }
        require(event.amount.scale() <= 2) { "amount scale must be 2 digits or fewer" }
        require(event.currency.length == 3) { "currency must be a three-letter code" }
        require(event.traceId.isNotBlank()) { "traceId must not be blank" }
        require(event.traceId.length <= 100) { "traceId must be 100 characters or fewer" }
    }
}
