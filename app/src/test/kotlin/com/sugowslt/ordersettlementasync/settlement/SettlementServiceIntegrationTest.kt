package com.sugowslt.ordersettlementasync.settlement

import com.sugowslt.ordersettlementasync.event.OrderCreatedEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
class SettlementServiceIntegrationTest {

    @Autowired
    private lateinit var settlementService: SettlementService

    @Autowired
    private lateinit var settlementRepository: SettlementRepository

    @BeforeEach
    fun cleanUp() {
        settlementRepository.deleteAll()
    }

    @Test
    fun `주문 이벤트를 처리하면 정산 원장이 생성된다`() {
        val result = settlementService.process(orderCreatedEvent())

        assertEquals(SettlementProcessResult.CREATED, result)
        val settlement = assertNotNull(settlementRepository.findByOrderId(1001L))
        assertEquals("evt-1", settlement.eventId)
        assertEquals(BigDecimal("15000.50"), settlement.amount)
        assertEquals(SettlementStatus.COMPLETED, settlement.status)
    }

    @Test
    fun `같은 이벤트를 다시 처리하면 정산 원장을 중복 생성하지 않는다`() {
        assertEquals(SettlementProcessResult.CREATED, settlementService.process(orderCreatedEvent()))

        val duplicateResult = settlementService.process(orderCreatedEvent())

        assertEquals(SettlementProcessResult.DUPLICATE, duplicateResult)
        assertEquals(1L, settlementRepository.count())
    }

    @Test
    fun `같은 멱등키의 다른 이벤트도 중복 처리한다`() {
        assertEquals(SettlementProcessResult.CREATED, settlementService.process(orderCreatedEvent()))

        val duplicateResult = settlementService.process(
            orderCreatedEvent(eventId = "evt-2", orderId = 1002L),
        )

        assertEquals(SettlementProcessResult.DUPLICATE, duplicateResult)
        assertEquals(1L, settlementRepository.count())
    }

    private fun orderCreatedEvent(
        eventId: String = "evt-1",
        orderId: Long = 1001L,
    ) = OrderCreatedEvent(
        eventId = eventId,
        occurredAt = "2026-08-18T06:00:00Z",
        traceId = "trace-1",
        orderId = orderId,
        userId = 2001L,
        amount = BigDecimal("15000.50"),
        currency = "KRW",
        idempotencyKey = "order-create-1001",
    )
}
