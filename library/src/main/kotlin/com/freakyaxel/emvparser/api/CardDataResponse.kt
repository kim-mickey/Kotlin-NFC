package com.freakyaxel.emvparser.api

sealed class CardDataResponse {
    data class Error(val error: CardReaderException) : CardDataResponse()
    object TagLost : CardDataResponse()
    data class CardNotSupported(val aids: List<String>) : CardDataResponse()
    data class Success(val cardData: CardData) : CardDataResponse()

    internal companion object {
        fun success(cardData: CardData) = Success(cardData)
        fun error(error: CardReaderException) = Error(error)
        fun tagLost() = TagLost
        fun cardNotSupported(aids: List<String> = emptyList()) = CardNotSupported(aids)
    }
}

fun <R> CardDataResponse.fold(
    onSuccess: (CardDataResponse.Success) -> R,
    onError: (CardDataResponse.Error) -> R,
    onTagLost: () -> R,
    onCardNotSupported: (CardDataResponse.CardNotSupported) -> R
): R = when (this) {
    is CardDataResponse.Success -> onSuccess(this)
    is CardDataResponse.Error -> onError(this)
    is CardDataResponse.CardNotSupported -> onCardNotSupported(this)
    CardDataResponse.TagLost -> onTagLost()
}

fun CardDataResponse.onError(
    block: (CardDataResponse.Error) -> Unit,
): CardDataResponse = when (this) {
    CardDataResponse.TagLost -> this
    is CardDataResponse.CardNotSupported -> this
    is CardDataResponse.Success -> this
    is CardDataResponse.Error -> {
        block(this)
        this
    }
}

fun CardDataResponse.onTagLost(
    block: () -> Unit,
): CardDataResponse = when (this) {
    is CardDataResponse.CardNotSupported -> this
    is CardDataResponse.Success -> this
    is CardDataResponse.Error -> this
    CardDataResponse.TagLost -> {
        block()
        this
    }
}

fun CardDataResponse.onCardNotSupported(
    block: (CardDataResponse.CardNotSupported) -> Unit,
): CardDataResponse = when (this) {
    CardDataResponse.TagLost -> this
    is CardDataResponse.Success -> this
    is CardDataResponse.Error -> this
    is CardDataResponse.CardNotSupported -> {
        block(this)
        this
    }
}

fun CardDataResponse.onSuccess(
    block: (CardDataResponse.Success) -> Unit,
): CardDataResponse = when (this) {
    CardDataResponse.TagLost -> this
    is CardDataResponse.CardNotSupported -> this
    is CardDataResponse.Error -> this
    is CardDataResponse.Success -> {
        block(this)
        this
    }
}