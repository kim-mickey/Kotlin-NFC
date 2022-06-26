package com.freakyaxel.emvparser.tlv

internal object TLVParser {
    fun getData(recv: ByteArray): ByteArray {
        val recvSize = recv.size
        return if (recvSize >= 2) {
            recv.copyOfRange(0, recvSize - 2)
        } else ByteArray(0)
    }
}