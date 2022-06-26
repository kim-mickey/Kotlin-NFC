package com.freakyaxel.emvparser.api

sealed class CardDataResponse {
    data class Error(val error: CardReaderException) : CardDataResponse()
    object TagLost : CardDataResponse()
    object CardNotSupported : CardDataResponse()
    data class Success(val cardData: CardData) : CardDataResponse()

    internal companion object {
        fun success(cardData: CardData) = Success(cardData)
        fun error(error: CardReaderException) = Error(error)
        fun tagLost() = TagLost
        fun cardNotSupported() = CardNotSupported
    }
}

fun <R> CardDataResponse.fold(
    onSuccess: (CardDataResponse.Success) -> R,
    onError: (CardDataResponse.Error) -> R,
    onTagLost: () -> R,
    onCardNotSupported: () -> R
): R = when (this) {
    is CardDataResponse.Success -> onSuccess(this)
    is CardDataResponse.Error -> onError(this)
    CardDataResponse.TagLost -> onTagLost()
    CardDataResponse.CardNotSupported -> onCardNotSupported()
}

fun CardDataResponse.onError(
    onError: (CardDataResponse.Error) -> Unit,
): CardDataResponse = when (this) {
    CardDataResponse.CardNotSupported -> this
    CardDataResponse.TagLost -> this
    is CardDataResponse.Success -> this
    is CardDataResponse.Error -> {
        onError(this)
        this
    }
}

fun CardDataResponse.onTagLost(
    block: () -> Unit,
): CardDataResponse = when (this) {
    CardDataResponse.CardNotSupported -> this
    is CardDataResponse.Success -> this
    is CardDataResponse.Error -> this
    CardDataResponse.TagLost -> {
        block.invoke()
        this
    }
}

fun CardDataResponse.onCardNotSupported(
    block: () -> Unit,
): CardDataResponse = when (this) {
    CardDataResponse.TagLost -> this
    is CardDataResponse.Success -> this
    is CardDataResponse.Error -> this
    CardDataResponse.CardNotSupported -> {
        block.invoke()
        this
    }
}

fun CardDataResponse.onSuccess(
    onSuccess: (CardDataResponse.Success) -> Unit,
): CardDataResponse = when (this) {
    CardDataResponse.CardNotSupported -> this
    CardDataResponse.TagLost -> this
    is CardDataResponse.Error -> this
    is CardDataResponse.Success -> {
        onSuccess(this)
        this
    }
}