#include <jni.h>
#include <cmath>
#include <vector>
#include <android/log.h>

#define TAG "HEX_DSP_ARM64"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

enum FilterType {
    PEAKING = 0,
    LOW_SHELF = 1,
    HIGH_SHELF = 2,
    LOW_PASS = 3,
    HIGH_PASS = 4,
    NOTCH = 5
};

struct BiquadCoeffs {
    double b0 = 1.0, b1 = 0.0, b2 = 0.0;
    double a1 = 0.0, a2 = 0.0;
};

static BiquadCoeffs calculateRBJ(int type, double freq, double sampleRate, double gainDb, double q) {
    BiquadCoeffs c;
    const double PI = 3.14159265358979323846;
    double w0 = 2.0 * PI * freq / sampleRate;
    double cosW0 = std::cos(w0);
    double sinW0 = std::sin(w0);
    double alpha = sinW0 / (2.0 * q);
    double A = std::pow(10.0, gainDb / 40.0);

    double a0 = 1.0;

    switch (type) {
        case PEAKING: {
            c.b0 = 1.0 + alpha * A;
            c.b1 = -2.0 * cosW0;
            c.b2 = 1.0 - alpha * A;
            a0   = 1.0 + alpha / A;
            c.a1 = -2.0 * cosW0;
            c.a2 = 1.0 - alpha / A;
            break;
        }
        case LOW_SHELF: {
            double sqrtA = std::sqrt(A);
            c.b0 = A * ((A + 1.0) - (A - 1.0) * cosW0 + 2.0 * sqrtA * alpha);
            c.b1 = 2.0 * A * ((A - 1.0) - (A + 1.0) * cosW0);
            c.b2 = A * ((A + 1.0) - (A - 1.0) * cosW0 - 2.0 * sqrtA * alpha);
            a0   = (A + 1.0) + (A - 1.0) * cosW0 + 2.0 * sqrtA * alpha;
            c.a1 = -2.0 * ((A - 1.0) + (A + 1.0) * cosW0);
            c.a2 = (A + 1.0) + (A - 1.0) * cosW0 - 2.0 * sqrtA * alpha;
            break;
        }
        case HIGH_SHELF: {
            double sqrtA = std::sqrt(A);
            c.b0 = A * ((A + 1.0) + (A - 1.0) * cosW0 + 2.0 * sqrtA * alpha);
            c.b1 = -2.0 * A * ((A - 1.0) + (A + 1.0) * cosW0);
            c.b2 = A * ((A + 1.0) + (A - 1.0) * cosW0 - 2.0 * sqrtA * alpha);
            a0   = (A + 1.0) - (A - 1.0) * cosW0 + 2.0 * sqrtA * alpha;
            c.a1 = 2.0 * ((A - 1.0) - (A + 1.0) * cosW0);
            c.a2 = (A + 1.0) - (A - 1.0) * cosW0 - 2.0 * sqrtA * alpha;
            break;
        }
        case LOW_PASS: {
            c.b0 = (1.0 - cosW0) / 2.0;
            c.b1 = 1.0 - cosW0;
            c.b2 = (1.0 - cosW0) / 2.0;
            a0   = 1.0 + alpha;
            c.a1 = -2.0 * cosW0;
            c.a2 = 1.0 - alpha;
            break;
        }
        case HIGH_PASS: {
            c.b0 = (1.0 + cosW0) / 2.0;
            c.b1 = -(1.0 + cosW0);
            c.b2 = (1.0 + cosW0) / 2.0;
            a0   = 1.0 + alpha;
            c.a1 = -2.0 * cosW0;
            c.a2 = 1.0 - alpha;
            break;
        }
        default:
            break;
    }

    c.b0 /= a0;
    c.b1 /= a0;
    c.b2 /= a0;
    c.a1 /= a0;
    c.a2 /= a0;

    return c;
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_com_techressive_hex_audio_BiquadFilter_calculateMagnitudeCurveNative(
    JNIEnv* env,
    jobject /* this */,
    jintArray types,
    jdoubleArray freqs,
    jdoubleArray gains,
    jdoubleArray qs,
    jbooleanArray enables,
    jint count,
    jdoubleArray evalFreqs,
    jint evalCount,
    jdouble sampleRate
) {
    jint* pTypes = env->GetIntArrayElements(types, nullptr);
    jdouble* pFreqs = env->GetDoubleArrayElements(freqs, nullptr);
    jdouble* pGains = env->GetDoubleArrayElements(gains, nullptr);
    jdouble* pQs = env->GetDoubleArrayElements(qs, nullptr);
    jboolean* pEnables = env->GetBooleanArrayElements(enables, nullptr);
    jdouble* pEvalFreqs = env->GetDoubleArrayElements(evalFreqs, nullptr);

    std::vector<BiquadCoeffs> activeCoeffs;
    activeCoeffs.reserve(count);

    for (int i = 0; i < count; ++i) {
        if (pEnables[i]) {
            activeCoeffs.push_back(calculateRBJ(pTypes[i], pFreqs[i], sampleRate, pGains[i], pQs[i]));
        }
    }

    jdoubleArray result = env->NewDoubleArray(evalCount);
    std::vector<double> outMagnitudes(evalCount);

    const double PI = 3.14159265358979323846;

    for (int k = 0; k < evalCount; ++k) {
        double f = pEvalFreqs[k];
        double totalDb = 0.0;
        double w = 2.0 * PI * f / sampleRate;
        double cosW = std::cos(w);
        double cos2W = std::cos(2.0 * w);
        double sinW = std::sin(w);
        double sin2W = std::sin(2.0 * w);

        for (const auto& c : activeCoeffs) {
            double numReal = c.b0 + c.b1 * cosW + c.b2 * cos2W;
            double numImag = -(c.b1 * sinW + c.b2 * sin2W);

            double denReal = 1.0 + c.a1 * cosW + c.a2 * cos2W;
            double denImag = -(c.a1 * sinW + c.a2 * sin2W);

            double numMagSq = numReal * numReal + numImag * numImag;
            double denMagSq = denReal * denReal + denImag * denImag;

            if (denMagSq > 1e-12) {
                double hMagSq = numMagSq / denMagSq;
                totalDb += 10.0 * std::log10(std::max(1e-12, hMagSq));
            }
        }
        outMagnitudes[k] = totalDb;
    }

    env->SetDoubleArrayRegion(result, 0, evalCount, outMagnitudes.data());

    env->ReleaseIntArrayElements(types, pTypes, JNI_ABORT);
    env->ReleaseDoubleArrayElements(freqs, pFreqs, JNI_ABORT);
    env->ReleaseDoubleArrayElements(gains, pGains, JNI_ABORT);
    env->ReleaseDoubleArrayElements(qs, pQs, JNI_ABORT);
    env->ReleaseBooleanArrayElements(enables, pEnables, JNI_ABORT);
    env->ReleaseDoubleArrayElements(evalFreqs, pEvalFreqs, JNI_ABORT);

    return result;
}
