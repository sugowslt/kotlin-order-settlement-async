package com.sugowslt.ordersettlementasync.api

import com.sugowslt.ordersettlementasync.event.OrderCreatedEvent
import com.sugowslt.ordersettlementasync.settlement.SettlementRepository
import com.sugowslt.ordersettlementasync.settlement.SettlementService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
class SettlementControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var settlementService: SettlementService

    @Autowired
    private lateinit var settlementRepository: SettlementRepository

    @BeforeEach
    fun cleanUp() {
        settlementRepository.deleteAll()
    }

    @Test
    fun `주문 번호로 정산 원장을 조회한다`() {
        settlementService.process(orderCreatedEvent())

        mockMvc.perform(get("/api/v1/settlements/{orderId}", 1001L))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.eventId").value("evt-1"))
            .andExpect(jsonPath("$.orderId").value(1001L))
            .andExpect(jsonPath("$.amount").value(15000.50))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
    }

    @Test
    fun `정산 원장이 없으면 404를 반환한다`() {
        mockMvc.perform(get("/api/v1/settlements/{orderId}", 9999L))
            .andExpect(status().isNotFound)
    }

    private fun orderCreatedEvent() = OrderCreatedEvent(
        eventId = "evt-1",
        occurredAt = "2026-08-18T06:00:00Z",
        traceId = "trace-1",
        orderId = 1001L,
        userId = 2001L,
        amount = BigDecimal("15000.50"),
        currency = "KRW",
        idempotencyKey = "order-create-1001",
    )
}
