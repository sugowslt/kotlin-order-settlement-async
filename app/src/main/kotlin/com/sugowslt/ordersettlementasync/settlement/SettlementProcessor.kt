package com.sugowslt.ordersettlementasync.settlement

import com.sugowslt.ordersettlementasync.event.OrderCreatedEvent

fun interface SettlementProcessor {
    fun process(event: OrderCreatedEvent): SettlementProcessResult
}

enum class SettlementProcessResult {
    CREATED,
    DUPLICATE,
}
