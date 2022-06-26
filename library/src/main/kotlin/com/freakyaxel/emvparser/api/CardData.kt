package com.freakyaxel.emvparser.api

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CardData internal constructor(
    val aid: List<String>,
    private val expectedDateFormat: DateFormat = EXPECTED_DATE_FORMAT,
    private val actualDateFormat: DateFormat = EXPECTED_DATE_FORMAT
) {

    constructor(
        aid: List<String>
    ) : this(aid, EXPECTED_DATE_FORMAT, ACTUAL_DATE_FORMAT)

    companion object {
        internal val EXPECTED_DATE_FORMAT = SimpleDateFormat("MM/yy", Locale.US)
        internal val ACTUAL_DATE_FORMAT = SimpleDateFormat("yyMMdd", Locale.US)
    }

    var formattedNumber: String? = null
        private set
    var formattedExpDate: String? = null
        private set
    var dateExpDate: Date? = null
        private set

    private var date: Date? = null

    var expDate: String? = null
        internal set(value) {
            if (value == null) return
            field = value
            date = actualDateFormat.parse(value)
            dateExpDate = date
            formattedExpDate = expectedDateFormat.format(date)
        }

    var number: String? = null
        internal set(value) {
            if (value == null) return
            val cleanValue = value.filter { it.isDigit() }
            field = cleanValue
            formattedNumber = cleanValue.chunked(4).joinToString(" ")
        }

    internal val isComplete: Boolean
        get() = number != null && expDate != null

    fun formattedExpDate(format: DateFormat): String? {
        return date?.let { format.format(it) }
    }

    override fun toString(): String {
        return """
            AID: ${aid.joinToString(" | ")}
            Number: $number
            FormattedNumber: $formattedNumber
            ExpDate: $expDate
            FormattedExpDate: $formattedExpDate
            IsComplete: $isComplete
        """
    }
}
