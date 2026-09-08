package com.example.music;

import be.tarsos.dsp.*;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.resample.RateTransposer;

import javax.sound.sampled.AudioFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioManipulationUtils {
    private AudioManipulationUtils(){}

    public static byte[] toByteArray(short[] samples) {
        byte[] bytes = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            bytes[i*2] = (byte) (samples[i] & 0xff);
            bytes[i*2+1] = (byte) ((samples[i] >> 8) & 0xff);
        }
        return bytes;
    }

    public static short[] toShortArray(byte[] audioBytes) {
        short[] samples = new short[audioBytes.length / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (short) ((audioBytes[i*2] & 0xff) | (audioBytes[i*2+1] << 8));
        }
        return samples;
    }

    //prevents errors from integers being over the max and under the min of the short object
    public static short toShort(int integer) {
        if (integer > Short.MAX_VALUE) {
            integer = Short.MAX_VALUE;
        } else if (integer < Short.MIN_VALUE){
            integer = Short.MIN_VALUE;
        }
        return (short)integer;
    }

    private static float[] toFloatArray(short[] samples) {
        float[] samplesFloat = new float[samples.length];
        for (int i = 0; i < samplesFloat.length; i++) {
            samplesFloat[i] = samples[i] / 32767f;
        }
        return samplesFloat;
    }

    private static short[] toShortArrayFromFloat(float[] samples) {
        short[] samplesShort = new short[samples.length];
        for (int i = 0; i < samplesShort.length; i++) {
            float sample = samples[i] * 32767f;
            if (sample > 32767f) sample = 32767f;
            if (sample < -32767f) sample = -32767f;
            samplesShort[i] = (short) sample;
        }
        return samplesShort;
    }

    public static short[] applyOps(boolean[] isOps, short[] samples, byte[] samplesByte, AudioFormat format, double startSec, double endSec,
                                   double volumeMultiplier, double pitchMultiplier){
        float sampleRate = format.getSampleRate();
        int channels = format.getChannels();
        if (isOps[0]){
            samples = trim(samples, sampleRate, channels, startSec, endSec);
        }
        if (isOps[1]){
            samples = volume(samples, volumeMultiplier);
        }
        if (isOps[2]){
            samples = pitch(samples, pitchMultiplier, format, channels);
        }

        return samples;
    }

    private static short[] trim(short[] samples, float sampleRate, int channels, double startSec, double endSec) {
        int startIndex = (int) Math.max(0, (startSec * sampleRate * channels));
        if (startIndex >= samples.length)
            startIndex = samples.length - 1;

        int endIndex = (int) Math.min(samples.length, (endSec * sampleRate * channels));
        if (endIndex <= startIndex)
            endIndex = startIndex + 1;

        return Arrays.copyOfRange(samples, startIndex, endIndex);
    }

    private static short[] volume(short[] samples, double multiplier){
        multiplier = (multiplier + 50) / 50;
        short[] output = new short[samples.length];
        for (int i = 0; i < samples.length; i++) {
            int newV = (int) (samples[i] * multiplier);
            output[i] = toShort(newV);
        }
        return output;
    }

    private static short[] pitch(short[] samples, double multiplier, AudioFormat format, int numChannels) {
        multiplier = 2 - ((multiplier + 50) / 50);

        if (numChannels == 1){
            return channelPitch(samples, multiplier, format);
        }

        short[][] channels = new short[numChannels][samples.length / numChannels];
        for (int i = 0; i < samples.length; i++) {
            channels[i % numChannels][i / numChannels] = samples[i];
        }

        AudioFormat newFormat = new AudioFormat(format.getSampleRate(), format.getSampleSizeInBits(), 1, true, format.isBigEndian());

        short[][] channelAfterPitched = new short[numChannels][];
        for (int i = 0; i < numChannels; i++) {
            channelAfterPitched[i] = channelPitch(channels[i], multiplier, newFormat);
        }

        int min = channelAfterPitched[0].length;
        for (int i = 0; i < numChannels; i++){
            if (min > channelAfterPitched[i].length){
                min = channelAfterPitched[i].length;
            }
        }
        short[] output = new short[min * numChannels];
        for (int i = 0; i < min; i++) {
            for (int j = 0; j < numChannels; j++) {
                output[i * numChannels + j] = channelAfterPitched[j][i];
            }
        }

        return output;
    }

    private static short[] channelPitch(short[] samples, double multiplier, AudioFormat format){
        byte[] samplesByte = toByteArray(samples);
        float sampleRate = format.getSampleRate();

        WaveformSimilarityBasedOverlapAdd wsboa = new WaveformSimilarityBasedOverlapAdd(
                WaveformSimilarityBasedOverlapAdd.Parameters.musicDefaults(multiplier, sampleRate));
        int overlap = wsboa.getOverlap();

        try {
            AudioDispatcher ad = AudioDispatcherFactory.fromByteArray(samplesByte, format, wsboa.getInputBufferSize(), overlap);
            wsboa.setDispatcher(ad);


            List<Float> modifiedSamplesFloat = new ArrayList<>();

            ad.addAudioProcessor(wsboa);
            ad.addAudioProcessor(new RateTransposer(multiplier));
            ad.addAudioProcessor(new AudioProcessor() {
                @Override
                public boolean process(AudioEvent event) {
                    for (float f : event.getFloatBuffer()) {
                        modifiedSamplesFloat.add(f);
                    }
                    return true;
                }

                @Override
                public void processingFinished() {}
            });
            ad.run();

            System.out.println("channelPitch: multiplier=" + multiplier + " in=" + samples.length + " out=" + modifiedSamplesFloat.size()
                    + " ratio=" + (modifiedSamplesFloat.size() / (double) samples.length));

            float[] modifiedFloatSamples = new float[modifiedSamplesFloat.size()];
            for (int i = 0; i < modifiedSamplesFloat.size(); i++) modifiedFloatSamples[i] = modifiedSamplesFloat.get(i);
            return toShortArrayFromFloat(modifiedFloatSamples);
        } catch (Exception e){
            System.out.println("audio dispatcher failed: " + e);
            return samples;
        }
    }
}
