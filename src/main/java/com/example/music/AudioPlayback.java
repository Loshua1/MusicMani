package com.example.music;

import com.example.music.data.Instance;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.Arrays;

public class AudioPlayback {
    private final AudioFormat format;
    private ArrayList<Instance> instances;
    private final SourceDataLine audioOutput;
    private long currFrame; // frame that i am playing
    private volatile boolean isPlaying;

    public AudioPlayback(AudioFormat format, ArrayList<Instance> instances) throws LineUnavailableException {
        this.format = format;
        this.instances = instances;
        audioOutput = AudioSystem.getSourceDataLine(format);
        audioOutput.open(format);
    }

    public void updateInstances(ArrayList<Instance> instances){
        this.instances = instances;
    }

    public void play(){
        if (isPlaying){
            return;
        }
        isPlaying = true;
        audioOutput.start();
        new Thread(this::playLoop).start();
    }

    public void stop(){
        isPlaying = false;
        audioOutput.stop();
        audioOutput.flush();
    }

    public boolean isPlaying(){return isPlaying;}

    public void setSec(double currFrame){
        this.currFrame = (long) (currFrame * format.getSampleRate() * format.getChannels());
    }

    public double getSec(){
        return(currFrame / format.getSampleRate()) / format.getChannels();
    }

    private void playLoop() {
        int frames = 1024;
        short[] mixOutput = new short[frames];

        while (isPlaying) {
            Arrays.fill(mixOutput, (short) 0);
            for (Instance instance : instances) {
                if (instance.getTrack().isEnabled()) {
                    short[] instAudio = instance.getAudio();
                    long instanceStartFrame = (long) (instance.getStartOffsetSec() * format.getSampleRate());
                    for (int i = 0; i < frames; i++) {
                        long overallFrame = currFrame + i;
                        long frameOfCurrInstance = overallFrame - instanceStartFrame;
                        if (frameOfCurrInstance >= 0 && frameOfCurrInstance < instAudio.length) {
                            int mixed = mixOutput[i] + instAudio[(int) frameOfCurrInstance];
                            mixOutput[i] = AudioManipulationUtils.toShort(mixed);
                        }
                    }
                }
            }

            byte[] outputByte = AudioManipulationUtils.toByteArray(mixOutput);
            audioOutput.write(outputByte, 0, outputByte.length);

            //advances time
            currFrame += frames;
        }
    }

}
