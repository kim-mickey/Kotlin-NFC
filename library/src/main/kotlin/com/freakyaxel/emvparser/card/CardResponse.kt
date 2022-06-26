package com.freakyaxel.emvparser.card

import com.freakyaxel.emvparser.tlv.TLVParser
import com.freakyaxel.emvparser.tlv.EmvTLVList

internal class CardResponse(val command: ByteArray, recv: ByteArray) {
    private val bytesData = TLVParser.getData(recv)
    private val statusWord = StatusWord(recv[recv.size - 2], recv[recv.size - 1])

    val data = EmvTLVList(bytesData)
    val isSuccess = statusWord.isSuccess
    val bytes: ByteArray = bytesData + byteArrayOf(statusWord.sw1, statusWord.sw2)
}