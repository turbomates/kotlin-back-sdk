package dev.tmsoft.lib.filter

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
            query.contains("~") -> convertToRangeValue()
            query.first() == '[' -> convertToListValue()
            query.first() == '{' -> convertToMapValue()
            else -> SingleValue(query)
        }
    }

    private fun convertToRangeValue(): RangeValue {
        return RangeValue(query.split("~").first().filterValue(), query.split("~")[1].filterValue())
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
                    values.add(SingleValue(tmpString))
                    tmpString = ""
                }
                else -> tmpString += currentChar
            }

            charIndex += 1
        }

        if (tmpString.isNotEmpty()) values.add(SingleValue(tmpString))
        return ListValue(values)
    }

    private fun convertToMapValue(): MapValue {
        val mapValue = mutableMapOf<String, Value>()
        var tmpMapItemKey = ""
        var tmpString = ""

        charIndex += 1
        while (query[charIndex] != '}') {
            when (currentChar) {
                '[' -> mapValue[tmpMapItemKey] = convertToListValue()
                '{' -> mapValue[tmpMapItemKey] = convertToMapValue()
                ':' -> {
                    tmpMapItemKey = tmpString
                    tmpString = ""
                }
                ',' -> if (tmpString.isNotEmpty()) {
                    mapValue[tmpMapItemKey] = SingleValue(tmpString)
                    tmpString = ""
                }
                else -> tmpString += currentChar
            }

            charIndex += 1
        }

        if (tmpString.isNotEmpty()) mapValue[tmpMapItemKey] = SingleValue(tmpString)
        return MapValue(mapValue)
    }

    private fun String.filterValue(): String? {
        return if (isBlank()) null else this
    }
}
