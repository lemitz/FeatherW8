package com.featherw8.player.audio

import androidx.media3.common.DataReader
import androidx.media3.extractor.DefaultExtractorInput
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DsdExtractorTest {
    @Test
    fun parsesDsfAndDeinterleavesBitReversedChannelBlocks() {
        val sampleRate = 2_822_400
        val channels = 2
        val sampleCount = 282_240L
        val blockSize = 64
        val validBytesPerChannel = ((sampleCount + 7L) / 8L).toInt()
        val storedBytesPerChannel = ((validBytesPerChannel + blockSize - 1) / blockSize) * blockSize
        val sourceChannels = Array(channels) { channel ->
            ByteArray(storedBytesPerChannel) { index ->
                if (index < validBytesPerChannel) (0x13 + channel * 37 + index * 11).toByte() else 0
            }
        }
        val inputBytes = makeDsf(sampleRate, channels, sampleCount, blockSize, sourceChannels)
        val input = extractorInput(inputBytes)

        val info = DsdFileInfo.parse(input)
        assertEquals(Container.DSF, info.container)
        assertEquals(channels, info.channels)
        assertEquals(sampleRate, info.sampleRate)
        assertEquals(sampleCount, info.sampleCountPerChannel)
        assertEquals(100_000L, info.durationUs)
        assertEquals(92L, info.dataStart)

        val output = ByteArray(blockSize * channels)
        val count = DsdDataReader(info).readInterleaved(input, output, 0, output.size)
        assertEquals(output.size, count)
        val expected = ByteArray(output.size)
        for (index in 0 until blockSize) {
            for (channel in 0 until channels) {
                expected[index * channels + channel] = reverseBits(sourceChannels[channel][index])
            }
        }
        assertArrayEquals(expected, output)
    }

    @Test
    fun parsesUncompressedDsdiffAndKeepsInterleavedDsdBytes() {
        val sampleRate = 2_822_400
        val channels = 2
        val audio = ByteArray(128) { index -> (index * 17 + 5).toByte() }
        val inputBytes = makeDsdiff(sampleRate, channels, audio)
        val input = extractorInput(inputBytes)

        val info = DsdFileInfo.parse(input)
        assertEquals(Container.DSDIFF, info.container)
        assertEquals(channels, info.channels)
        assertEquals(sampleRate, info.sampleRate)
        assertEquals(512L, info.sampleCountPerChannel)
        assertEquals(audio.size.toLong(), info.dataSize)

        val output = ByteArray(audio.size)
        val count = DsdDataReader(info).readInterleaved(input, output, 0, output.size)
        assertEquals(audio.size, count)
        assertArrayEquals(audio, output)
    }

    private fun extractorInput(bytes: ByteArray) =
        DefaultExtractorInput(ByteArrayDataReader(bytes), 0L, bytes.size.toLong())

    private class ByteArrayDataReader(private val bytes: ByteArray) : DataReader {
        private var position = 0

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (position >= bytes.size) return -1
            val count = minOf(length, bytes.size - position)
            bytes.copyInto(buffer, offset, position, position + count)
            position += count
            return count
        }
    }

    private fun makeDsf(
        sampleRate: Int,
        channels: Int,
        sampleCount: Long,
        blockSize: Int,
        sourceChannels: Array<ByteArray>
    ): ByteArray {
        val data = ByteArrayOutputStream()
        val blocks = sourceChannels.first().size / blockSize
        for (block in 0 until blocks) {
            for (channel in 0 until channels) {
                data.write(sourceChannels[channel], block * blockSize, blockSize)
            }
        }

        val format = ByteArray(40)
        putLe32(format, 0, 1)
        putLe32(format, 4, 0)
        putLe32(format, 8, if (channels == 2) 2 else channels)
        putLe32(format, 12, channels)
        putLe32(format, 16, sampleRate)
        putLe32(format, 20, 1)
        putLe64(format, 24, sampleCount)
        putLe32(format, 32, blockSize)
        putLe32(format, 36, 0)

        val fmtChunk = dsfChunk("fmt ", format)
        val dataChunk = dsfChunk("data", data.toByteArray())
        val fileSize = 28L + fmtChunk.size + dataChunk.size
        val header = ByteArrayOutputStream().apply {
            write("DSD ".toByteArray(Charsets.US_ASCII))
            write(le64(28L))
            write(le64(fileSize))
            write(le64(0L))
        }.toByteArray()
        return header + fmtChunk + dataChunk
    }

    private fun dsfChunk(id: String, payload: ByteArray): ByteArray = ByteArrayOutputStream().apply {
        write(id.toByteArray(Charsets.US_ASCII))
        write(le64(payload.size + 12L))
        write(payload)
    }.toByteArray()

    private fun makeDsdiff(sampleRate: Int, channels: Int, audio: ByteArray): ByteArray {
        val channelIds = if (channels == 2) {
            "SLFT".toByteArray(Charsets.US_ASCII) + "SRGT".toByteArray(Charsets.US_ASCII)
        } else {
            ByteArray(channels * 4)
        }
        val channelPayload = be16(channels) + channelIds
        val properties = "SND ".toByteArray(Charsets.US_ASCII) +
            dsdiffChunk("FS  ", be32(sampleRate.toLong())) +
            dsdiffChunk("CHNL", channelPayload) +
            dsdiffChunk("CMPR", "DSD ".toByteArray(Charsets.US_ASCII))
        val propertyChunk = dsdiffChunk("PROP", properties)
        val audioChunk = dsdiffChunk("DSD ", audio)
        val formBody = "DSD ".toByteArray(Charsets.US_ASCII) + propertyChunk + audioChunk
        return "FRM8".toByteArray(Charsets.US_ASCII) + be64(formBody.size.toLong()) + formBody
    }

    private fun dsdiffChunk(id: String, payload: ByteArray): ByteArray = ByteArrayOutputStream().apply {
        write(id.toByteArray(Charsets.US_ASCII))
        write(be64(payload.size.toLong()))
        write(payload)
        if (payload.size % 2 != 0) write(0)
    }.toByteArray()

    private fun le64(value: Long): ByteArray = ByteBuffer.allocate(8)
        .order(ByteOrder.LITTLE_ENDIAN).putLong(value).array()

    private fun be64(value: Long): ByteArray = ByteBuffer.allocate(8)
        .order(ByteOrder.BIG_ENDIAN).putLong(value).array()

    private fun be32(value: Long): ByteArray = ByteBuffer.allocate(4)
        .order(ByteOrder.BIG_ENDIAN).putInt(value.toInt()).array()

    private fun be16(value: Int): ByteArray = ByteBuffer.allocate(2)
        .order(ByteOrder.BIG_ENDIAN).putShort(value.toShort()).array()

    private fun putLe32(target: ByteArray, offset: Int, value: Int) {
        ByteBuffer.wrap(target, offset, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(value)
    }

    private fun putLe64(target: ByteArray, offset: Int, value: Long) {
        ByteBuffer.wrap(target, offset, 8).order(ByteOrder.LITTLE_ENDIAN).putLong(value)
    }

    private fun reverseBits(value: Byte): Byte {
        var input = value.toInt() and 0xff
        input = ((input and 0x55) shl 1) or ((input ushr 1) and 0x55)
        input = ((input and 0x33) shl 2) or ((input ushr 2) and 0x33)
        input = ((input and 0x0f) shl 4) or ((input ushr 4) and 0x0f)
        return input.toByte()
    }
}
