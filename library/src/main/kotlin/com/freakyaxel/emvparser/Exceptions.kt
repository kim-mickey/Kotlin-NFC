package com.freakyaxel.emvparser

class CardReaderException(
    override val message: String,
    cause: Throwable
) : Exception(message, cause)

internal class CardNotSupportedException private constructor(
    val aids: List<String>,
    override val message: String = "Card is not supported!"
) : Exception(message) {

    constructor() : this(emptyList<String>())

    constructor(
        aidBytes: ByteArray
    ) : this(listOf(aidBytes.toHex()))

    constructor(
        aidsBytes: List<ByteArray>
    ) : this(aidsBytes.map { it.toHex() })
}

internal fun Throwable.toCardReaderException(): CardReaderException =
    CardReaderException(message ?: "Unknown Error", this)