package com.example.music.data;

import com.example.music.AudioManipulationUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Track {
    private final File asset;
    private final byte[] defaultAssetByte;
    private final short[] defaultAssetShort;
    private final List<Instance> instances = new ArrayList<>();
    private final AudioFormat format;
    private final float sampleRate;
    private final int numChannels;
    private boolean isEnabled = true;

    public Track(String assetPath){
        format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100,
                16,
                2,
                4,
                44100,
                false
        );

        asset = new File(assetPath);
        try(AudioInputStream ais = AudioSystem.getAudioInputStream(asset);
            AudioInputStream newAis = AudioSystem.getAudioInputStream(format, ais))
        {
            defaultAssetByte = newAis.readAllBytes();
            sampleRate = format.getSampleRate();
            numChannels = format.getChannels();
        } catch (Exception e) {
            throw new RuntimeException("failed in getting format of file: " + assetPath, e);
        }
        //converting to short[]
        defaultAssetShort = AudioManipulationUtils.toShortArray(defaultAssetByte);
        addInstance(0);
    }

    public void addInstance(double startOffsetSec) {
        instances.add(new Instance(this, startOffsetSec, defaultAssetShort));
    }
//
//    public short[] getAudio(Operations operations) {
//        short[] editedShort = Arrays.copyOf(defaultAssetShort, defaultAssetShort.length);
//        boolean[] isOp = operations.getIsOp();
//        if ()
//        //implement other ops in utils
//        return editedShort;
//    }

    public Instance getLastInstance(){return instances.getLast();}

    public void flipEnabled(){
        isEnabled = !isEnabled;
    }
    public boolean isEnabled(){
        return isEnabled;
    }
    public List<Instance> getInstances(){
        return new ArrayList<>(instances);
    }
    public AudioFormat getFormat(){return format;}

    public String getFilePath(){return asset.getPath();}

    public int getNumChannels(){return numChannels;}

    public short[] getDefaultAssetShort(){
        return defaultAssetShort;
    }

    public byte[] getDefaultAssetByte(){
        return defaultAssetByte;
    }
}
