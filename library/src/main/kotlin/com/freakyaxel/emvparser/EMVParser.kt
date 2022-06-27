package com.freakyaxel.emvparser

import com.freakyaxel.emvparser.api.CardData
import com.freakyaxel.emvparser.api.CardDataResponse
import com.freakyaxel.emvparser.api.CardTag
import com.freakyaxel.emvparser.api.EMVReader
import com.freakyaxel.emvparser.api.EMVReaderLogger
import com.freakyaxel.emvparser.card.CardResponse
import com.freakyaxel.emvparser.card.PseDirectory
import com.freakyaxel.emvparser.tlv.EmvTLVList

internal class EMVParser(private val logger: EMVReaderLogger? = null) : EMVReader {

    private fun CardTag.selectPseDirectory(fileName: String): PseDirectory {
        log("[Step 1]", "Select $fileName to get the PSE directory")
        val fileNameBytes: ByteArray = fileName.toByteArray(Charsets.US_ASCII)
        val fileNameSizeByte = fileNameBytes.size.toByte()

        return getCardResponse(
            command = "00 A4 04 00" +           // Command
                    fileNameSizeByte.toHex() +  // Size
                    fileNameBytes.toHex() +     // Data
                    "00"                        // LE
        ).let {
            log(it.data)
            PseDirectory(it.data)
        }
    }

    override fun getCardData(cardTag: CardTag, handleConnection: Boolean): CardDataResponse =
        kotlin.runCatching {
            if (handleConnection) cardTag.connect()
            cardTag.selectMasterFile().also { log(it) }

            val pseDir = cardTag.getCardPseDirectory() ?: throw CardNotSupportedException()

            return cardTag.readCardData(pseDir).let {
                CardDataResponse.success(it)
            }.also {
                if (handleConnection) cardTag.disconnect()
            }
        }.getOrElse {
            when {
                it is CardNotSupportedException -> CardDataResponse.cardNotSupported(it.aids)
                it.message.orEmpty().contains("was lost") -> CardDataResponse.tagLost()
                else -> CardDataResponse.error(it.toCardReaderException())
            }
        }


    private fun CardTag.selectMasterFile(): CardResponse {
        log("[Step 0]", "SELECT FILE Master File (if available)")
        return getCardResponse(command = "00 A4 04 00")
    }

    private fun CardTag.getCardPseDirectory(): PseDirectory? {
        // 1PAY.SYS.DDF01 - for chip cards
        // 2PAY.SYS.DDF01 - for nfc cards
        val files = listOf("2PAY.SYS.DDF01")
        files.onEach {
            val dir = selectPseDirectory(it)
            if (dir.aids.isNotEmpty()) return dir
        }
        return null
    }

    private fun CardTag.readCardData(pseDirectory: PseDirectory): CardData {
        val aids = pseDirectory.aids
        val cardData = CardData(aid = aids.map { it.toHex() })
        val cardIsSupported = aids.map { aid ->
            selectAID(aid).also { aidReponse ->
                if (!cardData.isComplete && aidReponse.isSuccess) fillAllCardData(cardData)
            }.isSuccess
        }.any { it }
        if (!cardIsSupported) throw CardNotSupportedException(aids)
        return cardData
    }

    private fun CardTag.selectAID(aid: ByteArray): CardResponse {
        val aidSize: String = byteArrayOf(aid.size.toByte()).toHex(true)
        val aidAsHex: String = aid.toHex()

        log("[Step 2]", "Select Aid $aidAsHex")
        val cmd = "00 A4 04 00 $aidSize $aidAsHex 00"
        return getCardResponse(cmd)
    }

    private fun CardTag.fillAllCardData(cardData: CardData) {
        var doContinue = true

        // TODO: Understand how to make reading faster
        //  May available record be read from the `Emv41.PDOL` ?
        // Read this default record first. Reading my be faster. To be improved!
        readRecord(0x14, 0x01).takeIf { it.isSuccess }?.let {
            doContinue = !cardData.fillData(it.data)
        }

        var sfi = 1
        log("[Step 3.2]", "Read All Records")
        while (sfi <= 31 && doContinue) {
            var rec = 1
            log("Read record", "sfi $sfi/31")
            while (rec <= 16 && doContinue) {
                readRecord((sfi shl 3 or 4), rec).takeIf { it.isSuccess }?.let {
                    doContinue = !cardData.fillData(it.data)
                }
                rec++
            }
            sfi++
        }
    }

    private fun CardTag.readRecord(sfi: Int, rec: Int): CardResponse {
        log("    Read", "SFI $sfi record #$rec")
        return getCardResponse(
            command = "00 B2" +             // Read Record Command
                    rec.toByte().toHex() +  // Record ID
                    sfi.toByte().toHex() +  // Record SFI
                    "00",                   // LE
            log = false
        )
    }

    private fun CardTag.getCardResponse(command: String, log: Boolean = true): CardResponse {
        return getCardResponse(command.toByteArray(), log)
    }

    private fun CardTag.getCardResponse(command: ByteArray, log: Boolean = true): CardResponse {
        val recv: ByteArray = transceive(command)

        if (log) log("CMD:", command.toHex(true))
        if (log) log("CMD Recv", recv.toHex(true))

        if (log && recv.size > 2) {
            log("CMD Received", command.toHex(true))
        }

        return CardResponse(command, recv)
    }

    private fun log(parsedRecv: EmvTLVList) {
        parsedRecv.tags.onEachIndexed { index, tagTLV ->
            log("TLV [$index]", "$tagTLV")
        }
    }

    private fun log(cardResponse: CardResponse) {
        log(
            "Card Response",
            """
                Success: ${cardResponse.isSuccess}
                Send: ${cardResponse.command.toHex(true)}
                Recv: ${cardResponse.bytes}
            """.trimIndent()
        )
    }

    private fun log(key: String, value: ByteArray) {
        log(key, value.toHex(true))
    }

    private fun log(key: String, value: String) {
        logger?.emvLog(key, value)
    }

}
