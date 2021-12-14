package dev.tmsoft.lib.filter

class QueryConverter(private val str: String) {
    private var charIndex = 0
    private val currentChar
        get() = str[charIndex]

    companion object {
        fun convert(str: String): Value {
            val queryParser = QueryConverter(str)
            return queryParser.convertToValue()
        }
    }

    fun convertToValue(): Value {
        return when (str.first()) {
            '[' -> convertToListValue()
            '{' -> convertToMapValue()
            else -> SingleValue(str)
        }
    }

    private fun convertToListValue(): ListValue {
        val values = mutableListOf<Value>()
        var tmpString = ""

        charIndex += 1
        while (str[charIndex] != ']') {
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
        while (str[charIndex] != '}') {
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
}
