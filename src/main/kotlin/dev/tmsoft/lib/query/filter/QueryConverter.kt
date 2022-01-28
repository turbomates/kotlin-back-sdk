package dev.tmsoft.lib.query.filter

import dev.tmsoft.lib.query.ListValue
import dev.tmsoft.lib.query.MapValue
import dev.tmsoft.lib.query.RangeValue
import dev.tmsoft.lib.query.SingleValue
import dev.tmsoft.lib.query.Value

class QueryConverter(private val query: String) {
    private var charIndex = 0
    private val currentChar
        get() = query[charIndex]

    companion object {
        fun convert(values: List<String>): List<Value> {
            return values.map { convert(it) }
        }

        fun convert(query: String): Value {
            val queryParser = QueryConverter(query)
            return queryParser.convertToValue()
        }
    }

    private fun convertToValue(): Value {
        charIndex = 0
        return when {
            query.first() == '[' -> convertToListValue()
            query.first() == '{' -> convertToMapValue()
            else -> makeValueFromString(query)
        }
    }

    private fun makeValueFromString(value: String): Value {
        val splitValue = value.split("~")
        return if (splitValue.size == 1)
            SingleValue(value)
        else
            RangeValue(
                splitValue[0].ifBlank { null },
                splitValue[1].ifBlank { null }
            )
    }

    private fun convertToListValue(): ListValue {
        val values = mutableListOf<Value>()
        var tmpString = ""

        charIndex += 1
        while (query[charIndex] != ']') {
            when (currentChar) {
                '[' -> values.add(convertToListValue())
                '{' -> values.add(convertToMapValue())
                ',' -> if (tmpString.isNotEmpty()) {
                    values.add(makeValueFromString(tmpString))
                    tmpString = ""
                }
                else -> tmpString += currentChar
            }

            charIndex += 1
        }

        if (tmpString.isNotEmpty()) values.add(makeValueFromString(tmpString))
        return ListValue(values)
    }

    private fun convertToMapValue(): MapValue {
        val mapValue = mutableMapOf<String, Value>()
        var tmpMapItemKey = ""
        var tmpString = ""
        var isKeyParsing = true

        charIndex += 1
        while (query[charIndex] != '}') {
            when (currentChar) {
                '[' -> mapValue[tmpMapItemKey] = convertToListValue()
                '{' -> mapValue[tmpMapItemKey] = convertToMapValue()
                ':' -> {
                    if (isKeyParsing) {
                        isKeyParsing = false
                        tmpMapItemKey = tmpString
                        tmpString = ""
                    } else {
                        tmpString += currentChar
                    }
                }
                ',' -> {
                    if (tmpString.isNotEmpty()) {
                        mapValue[tmpMapItemKey] = makeValueFromString(tmpString)
                        tmpString = ""
                    }

                    isKeyParsing = true
                }

                else -> tmpString += currentChar
            }

            charIndex += 1
        }

        if (tmpString.isNotEmpty()) mapValue[tmpMapItemKey] = makeValueFromString(tmpString)
        return MapValue(mapValue)
    }
}
