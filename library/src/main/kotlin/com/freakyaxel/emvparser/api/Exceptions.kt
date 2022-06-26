package com.freakyaxel.emvparser.api

class CardReaderException(
    override val message: String,
    cause: Throwable
) : Exception(message, cause)

internal class CardNotSupportedException(
    override val message: String = "Card is not supported!"
) : Exception(message)

internal fun Throwable.toCardReaderException(): CardReaderException =
    CardReaderException(message ?: "Unknown Error", this)