package com.featherw8.player.audio

import androidx.media3.common.util.ParsableByteArray
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class DsdMetadataReaderTest {
    @Before
    fun skipMedia3TestDetection() {
        // ParsableByteArray probes the Android framework to find out whether it runs inside a test,
        // which cannot work in a plain JVM unit test (Build.FINGERPRINT is null in the stub jar).
        // Setting the flag explicitly skips that probe; on a device it is never reached either way.
        ParsableByteArray.setShouldEnforceLimitOnLegacyMethods(false)
    }

    private val sampleRate = 2_822_400
    private val channels = 2
    private val blockSize = 4096
    private val sampleCount = blockSize * 8L // exactly one block per channel

    @Test
    fun readsStreamParametersAndAsciiAndUtf16Tags() {
        val cover = byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55)
        val id3 = id3(
            textFrame("TIT2", "Back In Black"),
            utf16Frame("TPE1", "Пикник"),
            textFrame("TALB", "Telegram"),
            pictureFrame(cover)
        )
        val metadata = parse(dsf(id3))

        assertEquals(channels, metadata.channels)
        assertEquals(sampleRate, metadata.sampleRate)
        assertEquals(sampleCount * 1_000L / sampleRate, metadata.durationMs)
        assertEquals(5_644_800L, metadata.bitsPerSecond)
        assertEquals("DSD64", metadata.rateLabel)
        assertEquals("Back In Black", metadata.title)
        assertEquals("Пикник", metadata.artist)
        assertEquals("Telegram", metadata.album)
        assertArrayEquals(cover, metadata.coverBytes)
    }

    @Test
    fun dsfWithoutTagPointerStillReportsTheStream() {
        val metadata = parse(dsf(id3 = null))

        assertEquals(sampleRate, metadata.sampleRate)
        assertNull(metadata.title)
        assertNull(metadata.artist)
        assertNull(metadata.album)
        assertNull(metadata.coverBytes)
    }

    @Test
    fun readsId3ChunkOfDsdiff() {
        val id3 = id3(textFrame("TIT2", "Intro"), textFrame("TPE1", "Some Band"), textFrame("TALB", "Debut"))
        val metadata = parse(dsdiff(id3Chunk = id3))

        assertEquals(channels, metadata.channels)
        assertEquals(sampleRate, metadata.sampleRate)
        assertEquals("Intro", metadata.title)
        assertEquals("Some Band", metadata.artist)
        assertEquals("Debut", metadata.album)
    }

    @Test
    fun dsdiffFallsBackToDiinTitleAndArtist() {
        val metadata = parse(dsdiff(id3Chunk = null, diin = diin(title = "Old Title", artist = "Old Artist")))

        assertEquals("Old Title", metadata.title)
        assertEquals("Old Artist", metadata.artist)
        assertNull(metadata.album)
    }

    @Test
    fun namesTheDsdRate() {
        assertEquals("DSD64", dsdRateLabel(2_822_400))
        assertEquals("DSD128", dsdRateLabel(5_644_800))
        assertEquals("DSD256", dsdRateLabel(11_289_600))
        assertEquals("DSD64", dsdRateLabel(3_072_000))
    }

    // ---- helpers -------------------------------------------------------------------------

    private fun parse(bytes: ByteArray): DsdMetadata {
        val file = File.createTempFile("dsd-test", ".bin")
        try {
            file.writeBytes(bytes)
            return Files.newByteChannel(file.toPath(), StandardOpenOption.READ).use { channel ->
                DsdMetadataReader.parse(channel, strictTags = true)
            }
        } finally {
            file.delete()
        }
    }

    private fun dsf(id3: ByteArray?): ByteArray {
        val format = ByteArray(40)
        putLe32(format, 0, 1)
        putLe32(format, 8, 2)
        putLe32(format, 12, channels)
        putLe32(format, 16, sampleRate)
        putLe32(format, 20, 1)
        putLe64(format, 24, sampleCount)
        putLe32(format, 32, blockSize)
        val fmtChunk = dsfChunk("fmt ", format)
        val dataChunk = dsfChunk("data", ByteArray(blockSize * channels))
        val pointer = 28L + fmtChunk.size + dataChunk.size
        val header = ByteArrayOutputStream().apply {
            write("DSD ".toByteArray(Charsets.US_ASCII))
            write(le64(28L))
            write(le64(pointer + (id3?.size ?: 0)))
            write(le64(if (id3 != null) pointer else 0L))
        }.toByteArray()
        return header + fmtChunk + dataChunk + (id3 ?: ByteArray(0))
    }

    private fun dsfChunk(id: String, payload: ByteArray): ByteArray = id.toByteArray(Charsets.US_ASCII) +
        le64(payload.size + 12L) + payload

    private fun dsdiff(id3Chunk: ByteArray?, diin: ByteArray? = null): ByteArray {
        val properties = "SND ".toByteArray(Charsets.US_ASCII) +
            dsdiffChunk("FS  ", be32(sampleRate.toLong())) +
            dsdiffChunk("CHNL", be16(channels) + "SLFT".toByteArray() + "SRGT".toByteArray()) +
            dsdiffChunk("CMPR", "DSD ".toByteArray(Charsets.US_ASCII))
        var body = "DSD ".toByteArray(Charsets.US_ASCII) +
            dsdiffChunk("PROP", properties) +
            (if (diin != null) dsdiffChunk("DIIN", diin) else ByteArray(0)) +
            dsdiffChunk("DSD ", ByteArray(blockSize * channels))
        if (id3Chunk != null) body += dsdiffChunk("ID3 ", id3Chunk)
        return "FRM8".toByteArray(Charsets.US_ASCII) + be64(body.size.toLong()) + body
    }

    private fun dsdiffChunk(id: String, payload: ByteArray): ByteArray = ByteArrayOutputStream().apply {
        write(id.toByteArray(Charsets.US_ASCII))
        write(be64(payload.size.toLong()))
        write(payload)
        if (payload.size % 2 != 0) write(0)
    }.toByteArray()

    private fun diin(title: String, artist: String): ByteArray {
        fun text(id: String, value: String): ByteArray {
            val bytes = value.toByteArray(Charsets.UTF_8)
            return dsdiffChunk(id, be32(bytes.size.toLong()) + bytes)
        }
        return text("DITI", title) + text("DIAR", artist)
    }

    private fun id3(vararg frames: ByteArray): ByteArray {
        val body = frames.fold(ByteArray(0)) { all, frame -> all + frame }
        val size = byteArrayOf(
            ((body.size shr 21) and 0x7f).toByte(),
            ((body.size shr 14) and 0x7f).toByte(),
            ((body.size shr 7) and 0x7f).toByte(),
            (body.size and 0x7f).toByte()
        )
        return "ID3".toByteArray(Charsets.US_ASCII) + byteArrayOf(3, 0, 0) + size + body
    }

    private fun frame(id: String, payload: ByteArray): ByteArray =
        id.toByteArray(Charsets.US_ASCII) + ByteBuffer.allocate(4).putInt(payload.size).array() + byteArrayOf(0, 0) + payload

    private fun textFrame(id: String, text: String): ByteArray =
        frame(id, byteArrayOf(0) + text.toByteArray(Charsets.ISO_8859_1)) // encoding 0 = ISO-8859-1

    private fun utf16Frame(id: String, text: String): ByteArray =
        frame(id, byteArrayOf(1, 0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(Charsets.UTF_16LE)) // encoding 1 = UTF-16 + BOM

    private fun pictureFrame(image: ByteArray): ByteArray = frame(
        "APIC",
        byteArrayOf(0) + "image/jpeg".toByteArray(Charsets.US_ASCII) + byteArrayOf(0, 3, 0) + image
    )

    private fun le64(value: Long): ByteArray = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array()
    private fun be64(value: Long): ByteArray = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(value).array()
    private fun be32(value: Long): ByteArray = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value.toInt()).array()
    private fun be16(value: Int): ByteArray = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort(value.toShort()).array()
    private fun putLe32(target: ByteArray, offset: Int, value: Int) {
        ByteBuffer.wrap(target, offset, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(value)
    }
    private fun putLe64(target: ByteArray, offset: Int, value: Long) {
        ByteBuffer.wrap(target, offset, 8).order(ByteOrder.LITTLE_ENDIAN).putLong(value)
    }
}
