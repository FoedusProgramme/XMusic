package com.xapps.media.xmusic.stats;

import java.util.ArrayList;
import java.util.List;

public final class StatsFingerprinter {

    private static final int WINDOW_SIZE = 4096;
    private static final int HOP_SIZE = 2048;
    private static final int[] RANGE_BOUNDS = {10, 20, 40, 80, 160, 512};
    private static final int FUZZ_FACTOR = 2;

    public static List<Long> generateFingerprints(float[] pcmData, int sampleRate) {
        List<Long> fingerprints = new ArrayList<>();
        if (pcmData == null || pcmData.length < WINDOW_SIZE) {
            return fingerprints;
        }

        float[] window = createHannWindow();
        int totalSamples = pcmData.length;
        int frameCount = (totalSamples - WINDOW_SIZE) / HOP_SIZE + 1;
        
        int[][] peakMagnitudes = new int[frameCount][RANGE_BOUNDS.length - 1];

        for (int f = 0; f < frameCount; f++) {
            int startOffset = f * HOP_SIZE;
            float[] real = new float[WINDOW_SIZE];
            float[] imag = new float[WINDOW_SIZE];

            for (int i = 0; i < WINDOW_SIZE; i++) {
                real[i] = pcmData[startOffset + i] * window[i];
            }

            computeFft(real, imag);

            for (int r = 0; r < RANGE_BOUNDS.length - 1; r++) {
                float maxMag = -1f;
                int maxBin = RANGE_BOUNDS[r];

                for (int bin = RANGE_BOUNDS[r]; bin < RANGE_BOUNDS[r + 1]; bin++) {
                    float mag = (real[bin] * real[bin]) + (imag[bin] * imag[bin]);
                    if (mag > maxMag) {
                        maxMag = mag;
                        maxBin = bin;
                    }
                }
                peakMagnitudes[f][r] = maxBin;
            }
        }

        for (int f = 0; f < frameCount - 3; f++) {
            for (int r = 0; r < RANGE_BOUNDS.length - 2; r++) {
                long hash = hashPoints(
                        peakMagnitudes[f][r],
                        peakMagnitudes[f + 1][r],
                        peakMagnitudes[f + 2][r + 1],
                        peakMagnitudes[f + 3][r + 1]
                );
                fingerprints.add(hash);
            }
        }

        return fingerprints;
    }

    private static float[] createHannWindow() {
        float[] window = new float[WINDOW_SIZE];
        for (int i = 0; i < WINDOW_SIZE; i++) {
            window[i] = (float) (0.5 * (1.0 - Math.cos((2.0 * Math.PI * i) / (WINDOW_SIZE - 1))));
        }
        return window;
    }

    private static void computeFft(float[] real, float[] imag) {
        int n = real.length;
        int bits = (int) (Math.log(n) / Math.log(2));
        
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> (32 - bits);
            if (i < j) {
                float tempReal = real[i];
                real[i] = real[j];
                real[j] = tempReal;
                
                float tempImag = imag[i];
                imag[i] = imag[j];
                imag[j] = tempImag;
            }
        }

        for (int len = 2; len <= n; len <<= 1) {
            double angle = -2.0 * Math.PI / len;
            float wlenReal = (float) Math.cos(angle);
            float wlenImag = (float) Math.sin(angle);

            for (int i = 0; i < n; i += len) {
                float wReal = 1.0f;
                float wImag = 0.0f;

                for (int j = 0; j < len / 2; j++) {
                    int u = i + j;
                    int v = i + j + len / 2;

                    float tReal = real[v] * wReal - imag[v] * wImag;
                    float tImag = real[v] * wImag + imag[v] * wReal;

                    real[v] = real[u] - tReal;
                    imag[v] = imag[u] - tImag;
                    
                    real[u] += tReal;
                    imag[u] += tImag;

                    float nextWReal = wReal * wlenReal - wImag * wlenImag;
                    wImag = wReal * wlenImag + wImag * wlenReal;
                    wReal = nextWReal;
                }
            }
        }
    }

    private static long hashPoints(int p1, int p2, int p3, int p4) {
        p1 = (p1 / FUZZ_FACTOR) * FUZZ_FACTOR;
        p2 = (p2 / FUZZ_FACTOR) * FUZZ_FACTOR;
        p3 = (p3 / FUZZ_FACTOR) * FUZZ_FACTOR;
        p4 = (p4 / FUZZ_FACTOR) * FUZZ_FACTOR;

        long hash = 0;
        hash |= ((long) (p1 & 0xFF)) << 24;
        hash |= ((long) (p2 & 0xFF)) << 16;
        hash |= ((long) (p3 & 0xFF)) << 8;
        hash |= ((long) (p4 & 0xFF));
        return hash;
    }
}
