package com.xapps.media.xmusic.stats;

import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.BaseAudioProcessor;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

public final class StatsAudioProcessor extends BaseAudioProcessor {

    private static final int TARGET_DURATION_SEC = 20;

    private int channelCount;
    private int sampleRate;
    private int encoding;
    private int targetSampleCount;

    private float[] monoSamples = new float[65536];
    private int monoCount = 0;
    private boolean finishedEnough = false;

    @Override
    protected AudioFormat onConfigure(AudioFormat inputAudioFormat)
            throws UnhandledAudioFormatException {

        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT
                && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw new UnhandledAudioFormatException(inputAudioFormat);
        }

        channelCount = inputAudioFormat.channelCount;
        sampleRate = inputAudioFormat.sampleRate;
        encoding = inputAudioFormat.encoding;
        targetSampleCount = sampleRate * TARGET_DURATION_SEC;

        return inputAudioFormat;
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        if (!inputBuffer.hasRemaining() || finishedEnough) {
            inputBuffer.position(inputBuffer.limit());
            replaceOutputBuffer(0).flip();
            return;
        }

        if (encoding == C.ENCODING_PCM_16BIT) {
            processPcm16(inputBuffer);
        } else {
            processPcmFloat(inputBuffer);
        }

        inputBuffer.position(inputBuffer.limit());
        replaceOutputBuffer(0).flip();

        if (monoCount >= targetSampleCount) {
            finishedEnough = true;
        }
    }

    private void processPcm16(ByteBuffer inputBuffer) {
        ShortBuffer shortBuffer = inputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer();

        if (channelCount == 1) {
            ensureCapacity(monoCount + shortBuffer.remaining());

            while (shortBuffer.hasRemaining() && !finishedEnough) {
                monoSamples[monoCount++] = shortBuffer.get() / 32768f;
            }
        } else {
            int frames = shortBuffer.remaining() / channelCount;
            ensureCapacity(monoCount + frames);

            while (frames-- > 0 && !finishedEnough) {
                float sum = 0f;
                for (int ch = 0; ch < channelCount; ch++) {
                    sum += shortBuffer.get() / 32768f;
                }
                monoSamples[monoCount++] = sum / channelCount;
            }
        }
    }

    private void processPcmFloat(ByteBuffer inputBuffer) {
        FloatBuffer floatBuffer = inputBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer();

        if (channelCount == 1) {
            ensureCapacity(monoCount + floatBuffer.remaining());

            while (floatBuffer.hasRemaining() && !finishedEnough) {
                monoSamples[monoCount++] = floatBuffer.get();
            }
        } else {
            int frames = floatBuffer.remaining() / channelCount;
            ensureCapacity(monoCount + frames);

            while (frames-- > 0 && !finishedEnough) {
                float sum = 0f;
                for (int ch = 0; ch < channelCount; ch++) {
                    sum += floatBuffer.get();
                }
                monoSamples[monoCount++] = sum / channelCount;
            }
        }
    }

    private void ensureCapacity(int required) {
        if (required <= monoSamples.length) {
            return;
        }

        int newSize = monoSamples.length;
        while (newSize < required) {
            newSize *= 2;
        }

        monoSamples = Arrays.copyOf(monoSamples, newSize);
    }

    public boolean isFinishedEnough() {
        return finishedEnough;
    }

    public float[] getFinalMonoSamples() {
        return Arrays.copyOf(monoSamples, monoCount);
    }

    @Override
    protected void onFlush() {
        monoCount = 0;
        finishedEnough = false;
    }

    @Override
    protected void onReset() {
        monoCount = 0;
        finishedEnough = false;
        monoSamples = new float[65536];
    }

    public int getSampleRate() {
        return sampleRate;
    }
}
