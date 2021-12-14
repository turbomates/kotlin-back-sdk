package dev.tmsoft.lib.filter

import org.junit.jupiter.api.Test

class QueryConverterTest {
    @Test
    fun `query converter convert`() {
//        val tmpStr = "[[USD],EUR,TST]"
        val tmpStr = "{minAmount:100,rangeType:MIN,currencies:[USD,EUR]}"
        val result = QueryConverter.convert(tmpStr)
        println(result)
    }
}
