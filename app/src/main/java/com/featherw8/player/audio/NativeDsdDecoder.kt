package com.featherw8.player.audio

/** Converts complete 1/75-second interleaved DSD frames to signed 16-bit PCM. */
internal class NativeDsdDecoder private constructor(
    val channels: Int,
    val dsdSampleRate: Int,
    val pcmSampleRate: Int,
    private var handle: Long
) : AutoCloseable {
    val dsdBytesPerFrame: Int = (dsdSampleRate / 8 / DSD_FRAMES_PER_SECOND) * channels
    val pcmBytesPerFrame: Int = (pcmSampleRate / DSD_FRAMES_PER_SECOND) * channels * 2
    private val pcmBuffer = ByteArray(pcmBytesPerFrame)

    /** Returns the byte count written to [pcmOut], or a negative native error code. */
    fun convert(dsdFrame: ByteArray, pcmOut: ByteArray = pcmBuffer): Int {
        if (handle == 0L || dsdFrame.size != dsdBytesPerFrame || pcmOut.size < pcmBytesPerFrame) return -1
        return nativeConvert(handle, dsdFrame, pcmOut)
    }

    fun reusablePcmBuffer(): ByteArray = pcmBuffer

    override fun close() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0L
        }
    }

    private external fun nativeCreate(channels: Int, dsdSampleRate: Int, pcmSampleRate: Int): Long
    private external fun nativeConvert(handle: Long, dsdInput: ByteArray, pcmOutput: ByteArray): Int
    private external fun nativeDestroy(handle: Long)

    companion object {
        const val OUTPUT_SAMPLE_RATE = 88_200
        private const val DSD_FRAMES_PER_SECOND = 75
        private var loadFailure: Throwable? = null

        init {
            try {
                System.loadLibrary("feather_dsd")
            } catch (error: Throwable) {
                loadFailure = error
            }
        }

        fun open(channels: Int, dsdSampleRate: Int): NativeDsdDecoder? {
            if (loadFailure != null || channels !in 1..6 || dsdSampleRate <= 0 || dsdSampleRate % 600 != 0) return null
            val instance = NativeDsdDecoder(channels, dsdSampleRate, OUTPUT_SAMPLE_RATE, 0L)
            return try {
                val nativeHandle = instance.nativeCreate(channels, dsdSampleRate, OUTPUT_SAMPLE_RATE)
                if (nativeHandle == 0L) null else instance.apply { handle = nativeHandle }
            } catch (_: Throwable) {
                null
            }
        }
    }
}
