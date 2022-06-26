package com.freakyaxel.emvparser.card

import com.freakyaxel.emvparser.toHex

internal class StatusWord(
    val sw1: Byte,
    val sw2: Byte
) {

    private val bytes = byteArrayOf(sw1, sw2)

    val isSuccess: Boolean
        get() = sw1 == 0x90.toByte() && sw2 == 0x00.toByte()

    override fun toString() = bytes.toHex()
}