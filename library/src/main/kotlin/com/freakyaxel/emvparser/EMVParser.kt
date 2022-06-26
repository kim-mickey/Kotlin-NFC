package com.freakyaxel.emvparser

import com.freakyaxel.emvparser.api.*
import com.freakyaxel.emvparser.card.CardResponse
import com.freakyaxel.emvparser.card.PseDirectory
import com.freakyaxel.emvparser.tlv.EmvTLVList

internal class EMVParser(private val logger: EMVReaderLogger? = null) : EMVReader {

    private fun CardTag.selectPseDirectory(fileName: String): PseDirectory {
        log("[Step 1]", "Select $fileName to get the PSE directory")
        val fileNameAsBytes: ByteArray = fileName.toByteArray(Charsets.US_ASCII)
        val fileNameSize: String = byteArrayOf(fileNameAsBytes.size.toByte()).toHex()
        val fileNameAsHex: String = fileNameAsBytes.toHex()

        return getCardResponse("00 A4 04 00 $fileNameSize $fileNameAsHex 00").let {
            log(it.data)
            PseDirectory(it.data)
        }
    }

    override fun getCardData(cardTag: CardTag, handleConnection: Boolean): CardDataResponse =
        kotlin.runCatching {
            if (handleConnection) cardTag.connect()
            cardTag.selectMasterFile().also { log(it) }

            val pseDir = cardTag.getCardPseDirectory() ?: return CardDataResponse.cardNotSupported()

            return cardTag.readCardData(pseDir).let {
                CardDataResponse.success(it)
            }.also {
                cardTag.disconnect()
            }
        }.getOrElse {
            when {
                it.message.orEmpty().contains("was lost") -> CardDataResponse.tagLost()
                else -> CardDataResponse.error(it.toCardReaderException())
            }
        }


    private fun CardTag.selectMasterFile(): CardResponse {
        log("[Step 0]", "SELECT FILE Master File (if available)")
        return getCardResponse("00 A4 04 00")
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
        pseDirectory.aids.onEach { aid ->
            if (!cardData.isComplete) fillCardData(aid, cardData)
        }
        return cardData
    }

    private fun CardTag.fillCardData(aid: ByteArray, cardData: CardData) {
        val aidSize: String = byteArrayOf(aid.size.toByte()).toHex(true)
        val aidAsHex: String = aid.toHex()

        log("[Step 2]", "Select Aid $aidAsHex")
        val cmd = "00 A4 04 00 $aidSize $aidAsHex 00"
        val card = getCardResponse(cmd)

        if (card.isSuccess) {
            fillAllCardData(cardData)
        }
    }

    private fun CardTag.fillAllCardData(cardData: CardData) {
        var doContinue = true
        var sfi = 1
        log("[Step 3.2]", "Read All Records")
        while (sfi <= 31 && doContinue) {
            var rec = 1
            log("Read record", "sfi $sfi/31")
            while (rec <= 16 && doContinue) {
                readRecord(sfi, rec).takeIf { it.isSuccess }?.let {
                    doContinue = !cardData.fillData(it.data)
                }
                rec++
            }
            sfi++
        }
    }

    private fun CardTag.readRecord(sfi: Int, rec: Int): CardResponse {
        log("    Read", "SFI $sfi record #$rec")
        val readCmd = byteArrayOf(
            0x00,                       // READ_RECORD
            0xB2.toByte(),              // READ_RECORD
            rec.toByte(),               // record to read
            (sfi shl 3 or 4).toByte(),  // record sfi
            0x00                        // LE
        )
        return getCardResponse(readCmd, false)
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
