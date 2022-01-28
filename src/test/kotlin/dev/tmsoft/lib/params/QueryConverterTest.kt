package dev.tmsoft.lib.params

import dev.tmsoft.lib.params.filter.QueryConverter
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

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

    @Test
    fun `map value with datetime range data`() {
        val queryValues = listOf("{date:2021-12-28T00:00:00.000Z~2021-12-29T00:00:00.000Z,maxVal:~100,minVal:100~}")
        val (result) = QueryConverter.convert(queryValues)
        assertTrue { result is MapValue }
        result as MapValue
        assertTrue { result.value["date"] is RangeValue }
        val dateRangeValue = result.value["date"] as RangeValue
        assertEquals(dateRangeValue.from, "2021-12-28T00:00:00.000Z")
        assertEquals(dateRangeValue.to, "2021-12-29T00:00:00.000Z")

        assertTrue { result.value["maxVal"] is RangeValue }
        val maxValRangeValue = result.value["maxVal"] as RangeValue
        assertEquals(maxValRangeValue.from, null)
        assertEquals(maxValRangeValue.to, "100")
    }

    @Test
    fun `map value with array first element`() {
        val queryValues = listOf("{testKey:[testRangeFrom~],secondKey:secondValue}")
        val (result) = QueryConverter.convert(queryValues)
        assertTrue { result is MapValue }
        result as MapValue
        val testList = result.value["testKey"] as ListValue
        assertTrue { testList.values.first() is RangeValue }
        val dateRangeValue = testList.values.first() as RangeValue
        assertEquals(dateRangeValue.from, "testRangeFrom")
        assertTrue { result.value["secondKey"] is SingleValue }
    }
}
