package com.freakyaxel.emvparser.tlv

import com.freakyaxel.emvparser.tlv.tags.Emv41
import com.freakyaxel.emvparser.tlv.tags.ITag

internal data class TagTLV(
    override val key: Int,
    override val value: ByteArray,
    val parent: TagTLV?
) : ITag {

    private val children = arrayListOf<TagTLV>()

    val type: Emv41? = Emv41.getByTag(this.key)

    fun addChild(child: TagTLV) {
        children.add(child)
    }

    fun addAllChild(childList: Collection<TagTLV>) {
        children.addAll(childList)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TagTLV

        if (this.key != other.key) return false
        if (!value.contentEquals(other.value)) return false
        if (parent != other.parent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = this.key
        result = 31 * result + value.contentHashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        return result
    }
}