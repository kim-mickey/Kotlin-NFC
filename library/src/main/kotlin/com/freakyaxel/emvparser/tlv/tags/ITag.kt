package com.freakyaxel.emvparser.tlv.tags

import com.freakyaxel.emvparser.toHex

internal interface ITag {
    val key: Int
    val value: ByteArray

    val valueHex: String
        get() = value.toHex()
}