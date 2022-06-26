package com.freakyaxel.emvparser.api

import com.freakyaxel.emvparser.toByteArray

interface CardTag {
    fun transceive(command: String): ByteArray = transceive(command.toByteArray())
    fun transceive(command: ByteArray): ByteArray
    fun connect()
    fun disconnect()
    val connected: Boolean
}