#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <cmath>
#include <cstdarg>
#include <cstdint>
#include <new>
#include <vector>

#include <libdsdpcm/dsdpcm.h>

void log_printf(const char* format, ...) {
    va_list args;
    va_start(args, format);
    __android_log_vprint(ANDROID_LOG_WARN, "FeatherW8-DSD", format, args);
    va_end(args);
}

namespace {
constexpr size_t kDsdFramesPerSecond = 75;

struct DecoderHandle {
    dsdpcm_decoder_t* decoder = nullptr;
    size_t channels = 0;
    size_t dsdSampleRate = 0;
    size_t pcmSampleRate = 0;
    size_t inputBytesPerFrame = 0;
    size_t outputSamplesPerFrame = 0;
    std::vector<dsdpcm_sample64_t> pcm;

    ~DecoderHandle() {
        if (decoder != nullptr) {
            dsdpcm_destroy(decoder);
        }
    }
};

DecoderHandle* fromHandle(jlong value) {
    return reinterpret_cast<DecoderHandle*>(static_cast<uintptr_t>(value));
}
} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_featherw8_player_audio_NativeDsdDecoder_nativeCreate(
        JNIEnv*, jobject, jint channels, jint dsdSampleRate, jint pcmSampleRate) {
    if (channels < 1 || channels > 6 || dsdSampleRate <= 0 || pcmSampleRate <= 0 ||
        dsdSampleRate % static_cast<jint>(8 * kDsdFramesPerSecond) != 0 ||
        pcmSampleRate % static_cast<jint>(kDsdFramesPerSecond) != 0) {
        return 0;
    }

    auto* handle = new (std::nothrow) DecoderHandle();
    if (handle == nullptr) {
        return 0;
    }

    handle->channels = static_cast<size_t>(channels);
    handle->dsdSampleRate = static_cast<size_t>(dsdSampleRate);
    handle->pcmSampleRate = static_cast<size_t>(pcmSampleRate);
    handle->inputBytesPerFrame =
            (handle->dsdSampleRate / 8 / kDsdFramesPerSecond) * handle->channels;
    handle->outputSamplesPerFrame =
            (handle->pcmSampleRate / kDsdFramesPerSecond) * handle->channels;
    handle->pcm.resize(handle->outputSamplesPerFrame);
    handle->decoder = dsdpcm_create();
    if (handle->decoder == nullptr ||
        dsdpcm_init_multistage(handle->decoder,
                               handle->channels,
                               kDsdFramesPerSecond,
                               handle->dsdSampleRate,
                               handle->pcmSampleRate,
                               DSDPCM_PRECISION_FP64) != DSDPCM_OK) {
        delete handle;
        return 0;
    }

    return static_cast<jlong>(reinterpret_cast<uintptr_t>(handle));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_featherw8_player_audio_NativeDsdDecoder_nativeConvert(
        JNIEnv* env, jobject, jlong handleValue, jbyteArray dsdInput, jbyteArray pcmOutput) {
    auto* handle = fromHandle(handleValue);
    if (handle == nullptr || dsdInput == nullptr || pcmOutput == nullptr) {
        return -1;
    }

    const jsize inputLength = env->GetArrayLength(dsdInput);
    const jsize outputLength = env->GetArrayLength(pcmOutput);
    const size_t expectedOutputBytes = handle->outputSamplesPerFrame * sizeof(int16_t);
    if (static_cast<size_t>(inputLength) != handle->inputBytesPerFrame ||
        static_cast<size_t>(outputLength) < expectedOutputBytes) {
        return -2;
    }

    jbyte* input = env->GetByteArrayElements(dsdInput, nullptr);
    if (input == nullptr) {
        return -3;
    }

    size_t samplesWritten = 0;
    const int status = dsdpcm_convert_fp64(
            handle->decoder,
            reinterpret_cast<const uint8_t*>(input),
            handle->inputBytesPerFrame,
            handle->pcm.data(),
            &samplesWritten);
    env->ReleaseByteArrayElements(dsdInput, input, JNI_ABORT);

    if (status != DSDPCM_OK || samplesWritten != handle->outputSamplesPerFrame) {
        return -4;
    }

    jbyte* output = env->GetByteArrayElements(pcmOutput, nullptr);
    if (output == nullptr) {
        return -5;
    }

    for (size_t i = 0; i < samplesWritten; ++i) {
        const double sample = std::clamp(handle->pcm[i], -1.0, 1.0);
        const long quantized = std::lround(sample < 0.0 ? sample * 32768.0 : sample * 32767.0);
        const uint16_t packed = static_cast<uint16_t>(static_cast<int16_t>(quantized));
        output[i * 2] = static_cast<jbyte>(packed & 0xffU);
        output[i * 2 + 1] = static_cast<jbyte>((packed >> 8U) & 0xffU);
    }

    env->ReleaseByteArrayElements(pcmOutput, output, 0);
    return static_cast<jint>(expectedOutputBytes);
}

extern "C" JNIEXPORT void JNICALL
Java_com_featherw8_player_audio_NativeDsdDecoder_nativeDestroy(
        JNIEnv*, jobject, jlong handleValue) {
    delete fromHandle(handleValue);
}
