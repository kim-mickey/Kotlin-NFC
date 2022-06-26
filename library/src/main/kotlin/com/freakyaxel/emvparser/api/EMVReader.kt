package com.freakyaxel.emvparser.api

import com.freakyaxel.emvparser.EMVParser

interface EMVReader {
    fun getCardData(cardTag: CardTag, handleConnection: Boolean = true): CardDataResponse

    companion object {
        fun get(logger: EMVReaderLogger? = null): EMVReader = EMVParser(logger)
    }
}