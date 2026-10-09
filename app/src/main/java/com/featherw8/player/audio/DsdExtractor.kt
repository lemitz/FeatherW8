package com.featherw8.player.audio

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint
import androidx.media3.extractor.TrackOutput
import java.io.EOFException
import java.io.IOException
import kotlin.math.min

/**
 * Streaming Media3 extractor for uncompressed DSF and DSDIFF files.
 * DSD is converted to ordinary interleaved PCM before it reaches Android's audio sink.
 */
internal class DsdExtractor : Extractor {
    private var extractorOutput: ExtractorOutput? = null
    private var trackOutput: TrackOutput? = null
    private var info: DsdFileInfo? = null
    private var reader: DsdDataReader? = null
    private var decoder: NativeDsdDecoder? = null
    private var inputFrame = ByteArray(0)
    private var filledInputBytes = 0
    private var nextPcmFrame = 0L
    private var ended = false
    private var pendingSeekUs: Long? = null
    private var decoderFailed = false

    override fun sniff(input: ExtractorInput): Boolean {
        val signature = ByteArray(4)
        val supported = try {
            input.peekFully(signature, 0, signature.size, true) &&
                (signature.ascii(0, 4) == "DSD " || signature.ascii(0, 4) == "FRM8")
        } catch (_: EOFException) {
            false
        } finally {
            input.resetPeekPosition()
        }
        return supported
    }

    override fun init(output: ExtractorOutput) {
        extractorOutput = output
        trackOutput = output.track(0, C.TRACK_TYPE_AUDIO)
    }

    override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int {
        if (info == null) {
            // A restored position can be requested before the first sniff/read. Return to the
            // container header once, then honor the pending media-time seek after parsing it.
            if (input.position != 0L) {
                seekPosition.position = 0L
                return Extractor.RESULT_SEEK
            }
            val parsed = DsdFileInfo.parse(input)
            val nativeDecoder = NativeDsdDecoder.open(parsed.channels, parsed.sampleRate)
                ?: throw IOException("DSD-to-PCM decoder could not initialize this file")
            info = parsed
            decoder = nativeDecoder
            reader = DsdDataReader(parsed)
            inputFrame = ByteArray(nativeDecoder.dsdBytesPerFrame)
            trackOutput?.let { track ->
                track.format(
                    Format.Builder()
                        .setSampleMimeType(MimeTypes.AUDIO_RAW)
                        .setChannelCount(parsed.channels)
                        .setSampleRate(nativeDecoder.pcmSampleRate)
                        .setPcmEncoding(C.ENCODING_PCM_16BIT)
                        .setMaxInputSize(nativeDecoder.pcmBytesPerFrame)
                        .build()
                )
                track.durationUs(parsed.durationUs)
            }
            extractorOutput?.seekMap(DsdSeekMap(parsed, nativeDecoder.pcmSampleRate))
            extractorOutput?.endTracks()

            val requestedSeek = pendingSeekUs
            pendingSeekUs = null
            if (requestedSeek != null && requestedSeek > 0L) {
                val seekMap = DsdSeekMap(parsed, nativeDecoder.pcmSampleRate)
                val point = seekMap.getSeekPoints(requestedSeek).first
                applySeek(point.timeUs)
                seekPosition.position = point.position
                return Extractor.RESULT_SEEK
            }
        }

        if (decoderFailed) throw IOException("DSD-to-PCM decoder failed")
        if (ended) return Extractor.RESULT_END_OF_INPUT

        val parsed = info ?: return Extractor.RESULT_END_OF_INPUT
        val activeDecoder = decoder ?: throw IOException("DSD decoder is unavailable")
        val activeReader = reader ?: throw IOException("DSD stream reader is unavailable")

        while (filledInputBytes < inputFrame.size) {
            val count = activeReader.readInterleaved(
                input,
                inputFrame,
                filledInputBytes,
                inputFrame.size - filledInputBytes
            )
            if (count == 0) break
            filledInputBytes += count
        }

        if (filledInputBytes == 0) {
            ended = true
            return Extractor.RESULT_END_OF_INPUT
        }

        val isFinalPartialFrame = filledInputBytes < inputFrame.size
        if (isFinalPartialFrame) {
            // Complete the converter's 1/75-second input frame with the DSD silence pattern.
            inputFrame.fill(0x69.toByte(), filledInputBytes, inputFrame.size)
        }

        val pcm = activeDecoder.reusablePcmBuffer()
        val convertedBytes = activeDecoder.convert(inputFrame, pcm)
        if (convertedBytes <= 0) {
            decoderFailed = true
            throw IOException("DSD-to-PCM conversion failed ($convertedBytes)")
        }

        val bytesPerPcmFrame = parsed.channels * 2
        val convertedPcmFrames = convertedBytes / bytesPerPcmFrame
        val remainingPcmFrames = (parsed.totalPcmFrames - nextPcmFrame).coerceAtLeast(0L)
        if (remainingPcmFrames == 0L) {
            ended = true
            return Extractor.RESULT_END_OF_INPUT
        }
        val writtenPcmFrames = min(convertedPcmFrames.toLong(), remainingPcmFrames).toInt()
        val writtenBytes = writtenPcmFrames * bytesPerPcmFrame
        if (writtenBytes <= 0) {
            ended = true
            return Extractor.RESULT_END_OF_INPUT
        }

        val track = trackOutput ?: throw IOException("Audio track output is unavailable")
        track.sampleData(ParsableByteArray(pcm, writtenBytes), writtenBytes)
        val sampleTimeUs = nextPcmFrame * 1_000_000L / activeDecoder.pcmSampleRate
        track.sampleMetadata(sampleTimeUs, C.BUFFER_FLAG_KEY_FRAME, writtenBytes, 0, null)

        nextPcmFrame += writtenPcmFrames
        filledInputBytes = 0
        if (isFinalPartialFrame || nextPcmFrame >= parsed.totalPcmFrames) ended = true
        return Extractor.RESULT_CONTINUE
    }

    override fun seek(position: Long, timeUs: Long) {
        val parsed = info
        if (parsed == null) {
            pendingSeekUs = timeUs.coerceAtLeast(0L)
            return
        }
        applySeek(timeUs.coerceIn(0L, parsed.durationUs))
    }

    override fun release() {
        decoder?.close()
        decoder = null
        reader = null
    }

    private fun applySeek(timeUs: Long) {
        val parsed = info ?: return
        val oldDecoder = decoder
        oldDecoder?.close()
        val replacement = NativeDsdDecoder.open(parsed.channels, parsed.sampleRate)
        if (replacement == null) {
            decoderFailed = true
            return
        }
        decoder = replacement
        val targetFrame = (timeUs * replacement.pcmSampleRate / 1_000_000L)
            .coerceIn(0L, parsed.totalPcmFrames)
        nextPcmFrame = targetFrame
        filledInputBytes = 0
        ended = false
        decoderFailed = false
        reader?.seekToPcmFrame(targetFrame, replacement.pcmSampleRate)
    }
}

internal data class DsdFileInfo(
    val container: Container,
    val channels: Int,
    val sampleRate: Int,
    val sampleCountPerChannel: Long,
    val dataSize: Long,
    val dataStart: Long,
    val dsfBlockSize: Int
) {
    val validBytesPerChannel: Long = (sampleCountPerChannel + 7L) / 8L
    val totalPcmFrames: Long = sampleCountPerChannel * NativeDsdDecoder.OUTPUT_SAMPLE_RATE / sampleRate
    val durationUs: Long = sampleCountPerChannel * 1_000_000L / sampleRate

    fun dataPositionForByteOffset(byteOffset: Long): Long = when (container) {
        Container.DSF -> {
            val blockIndex = byteOffset / dsfBlockSize
            dataStart + blockIndex * dsfBlockSize.toLong() * channels
        }
        Container.DSDIFF -> dataStart + byteOffset * channels
    }

    companion object {
        fun parse(input: ExtractorInput): DsdFileInfo {
            val signature = readAscii(input, 4)
            return when (signature) {
                "DSD " -> parseDsf(input)
                "FRM8" -> parseDsdiff(input)
                else -> throw IOException("Unsupported DSD container")
            }
        }

        private fun parseDsf(input: ExtractorInput): DsdFileInfo {
            val dsdChunkSize = readLe64(input)
            if (dsdChunkSize < 28L) throw IOException("Invalid DSF header")
            skipFully(input, dsdChunkSize - 12L)

            var channels = 0
            var sampleRate = 0
            var sampleCount = 0L
            var blockSize = 4096

            while (true) {
                val chunkId = readAscii(input, 4)
                val chunkSize = readLe64(input)
                if (chunkSize < 12L) throw IOException("Invalid DSF chunk size")
                val contentSize = chunkSize - 12L
                when (chunkId) {
                    "fmt " -> {
                        if (contentSize < 40L) throw IOException("Invalid DSF format chunk")
                        val format = ByteArray(40)
                        input.readFully(format, 0, format.size)
                        channels = readLe32(format, 12).toInt()
                        sampleRate = readLe32(format, 16).toInt()
                        sampleCount = readLe64(format, 24)
                        blockSize = readLe32(format, 32).toInt()
                        if (contentSize > format.size) skipFully(input, contentSize - format.size)
                    }
                    "data" -> {
                        if (channels !in 1..6 || sampleRate <= 0 || sampleCount <= 0L || blockSize <= 0) {
                            throw IOException("DSF audio format information is missing or invalid")
                        }
                        val dataStart = input.position
                        val dataSize = contentSize
                        val info = DsdFileInfo(
                            Container.DSF, channels, sampleRate, sampleCount, dataSize, dataStart, blockSize
                        )
                        info.validate()
                        return info
                    }
                    else -> skipFully(input, contentSize)
                }
            }
        }

        private fun parseDsdiff(input: ExtractorInput): DsdFileInfo {
            val formSize = readBe64(input)
            val formType = readAscii(input, 4)
            if (formSize < 4L || formType !in setOf("DSD ", "DST ")) {
                throw IOException("Invalid DSDIFF FORM header")
            }
            if (formType == "DST ") {
                throw IOException("DST-compressed DSDIFF is not supported by this decoder")
            }
            val formEnd = 12L + formSize
            var channels = 0
            var sampleRate = 0
            var compression = "DSD "

            while (input.position + 12L <= formEnd) {
                val chunkId = readAscii(input, 4)
                val chunkSize = readBe64(input)
                if (chunkSize < 0L) throw IOException("Invalid DSDIFF chunk size")
                val contentStart = input.position
                val chunkEnd = safeAdd(contentStart, chunkSize)
                when (chunkId) {
                    "PROP" -> {
                        parseDsdiffProperties(input, chunkEnd) { id, value ->
                            when (id) {
                                "FS  " -> sampleRate = (value as Long).toInt()
                                "CHNL" -> channels = (value as Long).toInt()
                                "CMPR" -> compression = value as String
                            }
                        }
                    }
                    "DSD " -> {
                        if (channels !in 1..6 || sampleRate <= 0) {
                            throw IOException("DSDIFF channel or sample-rate metadata is missing")
                        }
                        if (compression != "DSD ") {
                            throw IOException("Unsupported DSDIFF compression: $compression")
                        }
                        val dataStart = input.position
                        val dataSize = chunkSize
                        if (dataSize % channels != 0L) throw IOException("Invalid DSDIFF audio data size")
                        val samples = (dataSize / channels) * 8L
                        val info = DsdFileInfo(
                            Container.DSDIFF, channels, sampleRate, samples, dataSize, dataStart, 4096
                        )
                        info.validate()
                        return info
                    }
                    "DST " -> throw IOException("DST-compressed DSDIFF is not supported by this decoder")
                    else -> skipFully(input, chunkSize)
                }
                skipTo(input, chunkEnd)
                if ((chunkSize and 1L) != 0L) skipFully(input, 1L)
            }
            throw IOException("DSDIFF audio data chunk was not found")
        }

        private fun parseDsdiffProperties(
            input: ExtractorInput,
            propEnd: Long,
            onValue: (String, Any) -> Unit
        ) {
            if (input.position + 4L > propEnd) throw IOException("Invalid DSDIFF properties chunk")
            readAscii(input, 4) // Property type, normally "SND ".
            while (input.position + 12L <= propEnd) {
                val id = readAscii(input, 4)
                val size = readBe64(input)
                if (size < 0L) throw IOException("Invalid DSDIFF property size")
                val contentStart = input.position
                val contentEnd = safeAdd(contentStart, size)
                when (id) {
                    "FS  " -> {
                        if (size >= 4L) onValue(id, readBe32(input))
                    }
                    "CHNL" -> {
                        if (size >= 2L) onValue(id, readBe16(input).toLong())
                    }
                    "CMPR" -> {
                        if (size >= 4L) onValue(id, readAscii(input, 4))
                    }
                }
                skipTo(input, contentEnd)
                if ((size and 1L) != 0L) skipFully(input, 1L)
            }
            skipTo(input, propEnd)
        }

        private fun DsdFileInfo.validate() {
            if (channels !in 1..6 || sampleRate <= 0 || sampleCountPerChannel <= 0L ||
                dataSize <= 0L || validBytesPerChannel <= 0L ||
                sampleRate % 600 != 0 || totalPcmFrames <= 0L) {
                throw IOException("Unsupported DSD channel count, sample rate, or duration")
            }
            val storedBytesPerChannel = dataSize / channels
            if (storedBytesPerChannel < validBytesPerChannel) {
                throw IOException("DSD audio payload is shorter than its declared sample count")
            }
        }
    }
}

internal class DsdDataReader(private val info: DsdFileInfo) {
    private var nextDsfBlock = 0L
    private var cachedBlock = ByteArray(0)
    private var cachedPosition = 0
    private var logicalBytePosition = 0L
    private var skipAtNextBlockBytes = 0

    fun seekToPcmFrame(pcmFrame: Long, pcmRate: Int) {
        val targetDsdBytes = (pcmFrame * info.sampleRate / pcmRate) / 8L
        cachedBlock = ByteArray(0)
        cachedPosition = 0
        when (info.container) {
            Container.DSF -> {
                val blockStart = targetDsdBytes / info.dsfBlockSize * info.dsfBlockSize
                nextDsfBlock = blockStart / info.dsfBlockSize
                logicalBytePosition = blockStart
                skipAtNextBlockBytes = ((targetDsdBytes - blockStart) * info.channels).toInt()
            }
            Container.DSDIFF -> {
                logicalBytePosition = targetDsdBytes
                nextDsfBlock = 0L
                skipAtNextBlockBytes = 0
            }
        }
    }

    @Throws(IOException::class)
    fun readInterleaved(
        input: ExtractorInput,
        target: ByteArray,
        targetOffset: Int,
        maxBytes: Int
    ): Int = when (info.container) {
        Container.DSDIFF -> readDsdiff(input, target, targetOffset, maxBytes)
        Container.DSF -> readDsf(input, target, targetOffset, maxBytes)
    }

    private fun readDsdiff(input: ExtractorInput, target: ByteArray, targetOffset: Int, maxBytes: Int): Int {
        val remaining = info.dataSize - logicalBytePosition * info.channels
        if (remaining <= 0L) return 0
        var count = min(maxBytes.toLong(), remaining).toInt()
        count -= count % info.channels
        if (count <= 0) return 0
        input.readFully(target, targetOffset, count)
        logicalBytePosition += count / info.channels
        return count
    }

    private fun readDsf(input: ExtractorInput, target: ByteArray, targetOffset: Int, maxBytes: Int): Int {
        var written = 0
        while (written < maxBytes) {
            if (cachedPosition >= cachedBlock.size) {
                if (!loadNextDsfBlock(input)) break
            }
            var count = min(maxBytes - written, cachedBlock.size - cachedPosition)
            count -= count % info.channels
            if (count <= 0) break
            System.arraycopy(cachedBlock, cachedPosition, target, targetOffset + written, count)
            cachedPosition += count
            logicalBytePosition += count / info.channels
            written += count
        }
        return written
    }

    private fun loadNextDsfBlock(input: ExtractorInput): Boolean {
        val blockStartByte = nextDsfBlock * info.dsfBlockSize
        if (blockStartByte >= info.validBytesPerChannel) return false
        val storedBytesPerChannel = info.dataSize / info.channels
        val storedRemaining = storedBytesPerChannel - blockStartByte
        if (storedRemaining <= 0L) return false
        val storedBlockBytes = min(info.dsfBlockSize.toLong(), storedRemaining).toInt()
        val validBlockBytes = min(storedBlockBytes.toLong(), info.validBytesPerChannel - blockStartByte).toInt()
        if (validBlockBytes <= 0) return false

        val channelBlocks = Array(info.channels) { ByteArray(storedBlockBytes) }
        channelBlocks.forEach { input.readFully(it, 0, it.size) }
        val interleaved = ByteArray(validBlockBytes * info.channels)
        for (byteIndex in 0 until validBlockBytes) {
            val outputOffset = byteIndex * info.channels
            for (channel in 0 until info.channels) {
                interleaved[outputOffset + channel] = reverseBits(channelBlocks[channel][byteIndex])
            }
        }

        nextDsfBlock++
        cachedBlock = interleaved
        cachedPosition = skipAtNextBlockBytes.coerceIn(0, interleaved.size)
        skipAtNextBlockBytes = 0
        logicalBytePosition = blockStartByte + cachedPosition / info.channels
        return cachedPosition < cachedBlock.size
    }
}

private class DsdSeekMap(
    private val info: DsdFileInfo,
    private val pcmSampleRate: Int
) : SeekMap {
    override fun isSeekable(): Boolean = true
    override fun getDurationUs(): Long = info.durationUs

    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
        val clampedUs = timeUs.coerceIn(0L, info.durationUs)
        val pcmFrame = (clampedUs * pcmSampleRate / 1_000_000L).coerceIn(0L, info.totalPcmFrames)
        val actualTimeUs = pcmFrame * 1_000_000L / pcmSampleRate
        val dsdByteOffset = (pcmFrame * info.sampleRate / pcmSampleRate) / 8L
        val position = info.dataPositionForByteOffset(dsdByteOffset)
        return SeekMap.SeekPoints(SeekPoint(actualTimeUs, position))
    }
}

internal enum class Container { DSF, DSDIFF }

private fun reverseBits(value: Byte): Byte {
    var input = value.toInt() and 0xff
    input = ((input and 0x55) shl 1) or ((input ushr 1) and 0x55)
    input = ((input and 0x33) shl 2) or ((input ushr 2) and 0x33)
    input = ((input and 0x0f) shl 4) or ((input ushr 4) and 0x0f)
    return input.toByte()
}

private fun readAscii(input: ExtractorInput, count: Int): String {
    val bytes = ByteArray(count)
    input.readFully(bytes, 0, count)
    return bytes.toString(Charsets.US_ASCII)
}

private fun ByteArray.ascii(offset: Int, count: Int): String =
    String(this, offset, count, Charsets.US_ASCII)

private fun readLe64(input: ExtractorInput): Long {
    val bytes = ByteArray(8)
    input.readFully(bytes, 0, 8)
    var result = 0L
    for (index in 7 downTo 0) result = (result shl 8) or (bytes[index].toLong() and 0xffL)
    if (result < 0L) throw IOException("DSF chunk exceeds the supported file size")
    return result
}

private fun readLe64(bytes: ByteArray, offset: Int): Long {
    var result = 0L
    for (index in 7 downTo 0) result = (result shl 8) or (bytes[offset + index].toLong() and 0xffL)
    if (result < 0L) throw IOException("DSF field exceeds the supported file size")
    return result
}

private fun readBe64(input: ExtractorInput): Long {
    val bytes = ByteArray(8)
    input.readFully(bytes, 0, 8)
    var result = 0L
    for (index in 0..7) result = (result shl 8) or (bytes[index].toLong() and 0xffL)
    if (result < 0L) throw IOException("DSDIFF chunk exceeds the supported file size")
    return result
}

private fun readLe32(bytes: ByteArray, offset: Int): Long =
    (bytes[offset].toLong() and 0xffL) or
        ((bytes[offset + 1].toLong() and 0xffL) shl 8) or
        ((bytes[offset + 2].toLong() and 0xffL) shl 16) or
        ((bytes[offset + 3].toLong() and 0xffL) shl 24)

private fun readBe32(input: ExtractorInput): Long {
    val bytes = ByteArray(4)
    input.readFully(bytes, 0, 4)
    return ((bytes[0].toLong() and 0xffL) shl 24) or
        ((bytes[1].toLong() and 0xffL) shl 16) or
        ((bytes[2].toLong() and 0xffL) shl 8) or
        (bytes[3].toLong() and 0xffL)
}

private fun readBe16(input: ExtractorInput): Int {
    val bytes = ByteArray(2)
    input.readFully(bytes, 0, 2)
    return ((bytes[0].toInt() and 0xff) shl 8) or (bytes[1].toInt() and 0xff)
}

@Throws(IOException::class)
private fun skipFully(input: ExtractorInput, byteCount: Long) {
    if (byteCount < 0L) throw IOException("Invalid chunk boundary")
    var remaining = byteCount
    while (remaining > 0L) {
        val step = min(remaining, Int.MAX_VALUE.toLong()).toInt()
        input.skipFully(step)
        remaining -= step
    }
}

@Throws(IOException::class)
private fun skipTo(input: ExtractorInput, targetPosition: Long) {
    val remaining = targetPosition - input.position
    if (remaining < 0L) throw IOException("Invalid chunk layout")
    skipFully(input, remaining)
}

private fun safeAdd(left: Long, right: Long): Long {
    if (right < 0L || left > Long.MAX_VALUE - right) throw IOException("Invalid chunk size")
    return left + right
}
