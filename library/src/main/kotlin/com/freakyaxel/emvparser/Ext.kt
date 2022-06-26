package com.freakyaxel.emvparser

import com.freakyaxel.emvparser.api.CardData
import com.freakyaxel.emvparser.tlv.EmvTLVList
import com.freakyaxel.emvparser.tlv.tags.Emv41

internal fun CardData.fillData(data: EmvTLVList): Boolean = this.apply {
    expDate = data.getTlV(Emv41.Application_Expiration_Date)?.valueHex ?: this.expDate
    number =
        data.getTlV(Emv41.Application_Primary_Account_Number_PAN)?.valueHex ?: this.number
}.isComplete

internal fun ByteArray.toHex(space: Boolean = false): String =
    joinToString(separator = if (space) " " else "") { eachByte ->
        "%02x".format(eachByte)
    }.uppercase()

internal fun String.toByteArray(): ByteArray = with(this.removeWhitespaces()) {
    check(length % 2 == 0) { "Must have an even length" }

    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

internal fun ByteArray.concat(array: ByteArray): ByteArray {
    val concatArray = ByteArray(this.size + array.size)
    System.arraycopy(this, 0, concatArray, 0, this.size)
    System.arraycopy(array, 0, concatArray, this.size, array.size)
    return concatArray
}

internal fun String.removeWhitespaces() = filter { !it.isWhitespace() }
