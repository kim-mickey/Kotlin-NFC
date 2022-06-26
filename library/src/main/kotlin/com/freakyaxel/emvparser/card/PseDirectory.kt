package com.freakyaxel.emvparser.card

import com.freakyaxel.emvparser.tlv.EmvTLVList
import com.freakyaxel.emvparser.tlv.tags.Emv41

internal class PseDirectory(data: EmvTLVList) {
    val aids = data.getTlVs(Emv41.DF_ADF_NAME).map { it.value }
}