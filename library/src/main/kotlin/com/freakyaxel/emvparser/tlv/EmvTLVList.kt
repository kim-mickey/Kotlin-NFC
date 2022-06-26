package com.freakyaxel.emvparser.tlv

import com.freakyaxel.emvparser.concat
import com.freakyaxel.emvparser.tlv.tags.Emv41
import com.freakyaxel.emvparser.tlv.tags.Emv41Type
import com.freakyaxel.emvparser.tlv.tags.ITag
import java.math.BigInteger
import java.nio.ByteBuffer

internal class EmvTLVList(buf: ByteArray, offset: Int = 0) {
    private val _tags: MutableList<TagTLV> = ArrayList()

    val tags: List<TagTLV>
        get() = _tags

    init {
        unpack(buf, offset)
    }

    fun getTlVs(tag: Emv41): Array<TagTLV> {
        return getTlVs(tag.key)
    }

    fun getTlVs(tag: ITag): Array<TagTLV> {
        return getTlVs(tag.key)
    }

    fun getTlV(tag: ITag): TagTLV? {
        return getTlV(tag.key)
    }

    fun getTlV(tag: Emv41): TagTLV? {
        return getTlV(tag.key)
    }

    fun containsKey(tag: TagTLV): Boolean {
        return containsKey(tag.key)
    }

    private fun getTlV(key: Int): TagTLV? {
        return tags.find { it.key == key }
    }

    private fun getTlVs(key: Int): Array<TagTLV> {
        return tags.filter { it.key == key }.toTypedArray()
    }

    private fun containsKey(key: Int): Boolean {
        return tags.any { it.key == key }
    }

    // region Parser

    private fun unpack(buf: ByteArray, offset: Int) {
        val buffer: ByteBuffer = ByteBuffer.wrap(buf, offset, buf.size - offset)
        parseTLVListLInDept(buffer, null)
    }

    private fun parseTLVListLInDept(buf: ByteArray, parentKey: TagTLV): ArrayList<TagTLV> {
        val buffer: ByteBuffer = ByteBuffer.wrap(buf, 0, buf.size)
        return parseTLVListLInDept(buffer, parentKey)
    }

    private fun parseTLVListLInDept(buffer: ByteBuffer, parent: TagTLV?): ArrayList<TagTLV> {
        val levelTags = ArrayList<TagTLV>()
        while (hasNext(buffer)) {
            val currentNode = getTLVMsg(buffer, parent) ?: continue

            val emv: Emv41? = Emv41.getByTag(currentNode.key)

            when (emv?.type) {
                Emv41Type.TLV -> {
                    val sub = parseTLVListLInDept(currentNode.value, currentNode)
                    currentNode.addAllChild(sub)

                    levelTags.add(currentNode)
                    _tags.add(currentNode)
                }
                else -> {
                    levelTags.add(currentNode)
                    _tags.add(currentNode)
                }
            }
        }
        return levelTags
    }

    // endregion

    companion object {
        /**
         * Read next TLV Message from stream and return it
         *
         * @param buffer
         * @return TLVMsg
         */
        private fun getTLVMsg(buffer: ByteBuffer, parent: TagTLV?): TagTLV? {
            val tag = getTAG(buffer)
            if (tag == 0) return null

            if (!buffer.hasRemaining()) throw RuntimeException(
                String.format("BAD TLV FORMAT - tag (%x) without length or value", tag)
            )
            val length = getValueLength(buffer)
            if (length > buffer.remaining()) throw RuntimeException(
                String.format(
                    "BAD TLV FORMAT - tag (%x) length (%d) exceeds available data.",
                    tag, length
                )
            )
            val arrValue = ByteArray(length)
            buffer.get(arrValue)
            return TagTLV(tag, arrValue, parent)
        }

        /**
         * Read length bytes and return the int value
         *
         * @param buffer
         * @return value length
         */
        private fun getValueLength(buffer: ByteBuffer): Int {
            val b = buffer.get().toInt()
            val count = b and 0x7f
            // check first byte for more bytes to follow
            if ((b and 0x80) == 0 || count == 0) return count

            //fetch rest of bytes
            var bb = ByteArray(count)
            buffer.get(bb)
            //adjust buffer if first bit is turn on
            //important for BigInteger representation
            if ((bb[0].toInt() and 0x80) > 0) bb = ByteArray(1).concat(bb)
            return BigInteger(bb).toInt()
        }

        /**
         * Return the next TAG
         *
         * @return tag
         */
        private fun getTAG(buffer: ByteBuffer): Int {
            var b: Int
            b = buffer.get().toInt() and 0xff
            // Skip padding chars
            if (b == 0xFF || b == 0x00) {
                do {
                    b = buffer.get().toInt() and 0xff
                } while ((b == 0xFF || b == 0x00) && hasNext(buffer))
            }
            // Get first byte of Tag Identifier
            var tag = b.toInt()
            // Get rest of Tag identifier if required
            if ((b and 0x1F) == 0x1F) {
                do {
                    tag = tag shl 8
                    b = buffer.get().toInt()
                    tag = tag or (b and 0xFF)
                } while ((b and 0x80) == 0x80)
            }
            return tag
        }

        /**
         * Check Existance of next TLV Field
         *
         * @param buffer ByteBuffer containing TLV data
         */
        private fun hasNext(buffer: ByteBuffer): Boolean {
            return buffer.hasRemaining()
        }
    }
}