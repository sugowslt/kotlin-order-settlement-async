package com.sugowslt.ordersettlementasync.api

import com.sugowslt.ordersettlementasync.settlement.Settlement
import com.sugowslt.ordersettlementasync.settlement.SettlementService
import com.sugowslt.ordersettlementasync.settlement.SettlementStatus
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/settlements")
class SettlementController(
    private val settlementService: SettlementService,
) {

    @GetMapping("/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    fun getSettlement(@PathVariable orderId: Long): SettlementResponse {
        val settlement = settlementService.getByOrderId(orderId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "settlement not found")
        return SettlementResponse.from(settlement)
    }
}

data class SettlementResponse(
    val id: Long,
    val eventId: String,
    val orderId: Long,
    val userId: Long,
    val amount: BigDecimal,
    val currency: String,
    val status: SettlementStatus,
    val traceId: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(settlement: Settlement) = SettlementResponse(
            id = requireNotNull(settlement.id),
            eventId = settlement.eventId,
            orderId = settlement.orderId,
            userId = settlement.userId,
            amount = settlement.amount,
            currency = settlement.currency,
            status = settlement.status,
            traceId = settlement.traceId,
            createdAt = settlement.createdAt,
        )
    }
}
