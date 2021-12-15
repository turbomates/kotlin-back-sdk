package dev.tmsoft.lib.filter

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueryConverterTest {
    @Test
    fun `single value`() {
        val queryValues = listOf("test")
        val (result) = QueryConverter.convert(queryValues)
        assertTrue { result is SingleValue }
        result as SingleValue
        assertEquals("test", result.value)
    }

    @Test
    fun `range value`() {
        val queryValues = listOf("from~to")
        val (result) = QueryConverter.convert(queryValues)
        assertTrue { result is RangeValue }
        result as RangeValue
        assertEquals("from", result.from)
        assertEquals("to", result.to)
    }

    @Test
    fun `complex value`() {
        val queryValues = listOf("[0,1,2]", "{testKey:{innerTestKey:[arr,[innerArr,{inArrKey:inArrVal}],testArr]}}")
        val (listResult, mapResult) = QueryConverter.convert(queryValues)
        assertTrue { listResult is ListValue }
        listResult as ListValue
        assertEquals(3, listResult.values.size)
        val secondListResultValue = listResult.values[1]
        assertTrue { secondListResultValue is SingleValue && secondListResultValue.value == "1" }

        assertTrue { mapResult is MapValue }
        mapResult as MapValue
        assertEquals(1, mapResult.value.size)
        assertEquals("testKey", mapResult.value.keys.first())

        val firstMapResultValue = mapResult.value["testKey"]
        assertTrue {
            firstMapResultValue is MapValue
                    && firstMapResultValue.value.keys.first() == "innerTestKey"
                    && firstMapResultValue.value.values.first() is ListValue
        }
    }
}
