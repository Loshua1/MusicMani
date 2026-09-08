package com.example.music.data;

//import java.util.UUID;

import com.example.music.AudioManipulationUtils;

import javax.sound.sampled.AudioFormat;

public class Instance {
    private short[] savedAudio;
    private int savedAudioVersion = 0;
    private final Track track;
//    private final String instanceID;
    private double startOffsetSec;
    private final Operations operations;

    public Instance(Track track, double startOffsetSec, short[] savedAudio){
//        instanceID = UUID.randomUUID().toString();
        this.startOffsetSec = startOffsetSec;
        this.track = track;
        this.savedAudio = savedAudio;
        operations = new Operations();
    }

    public short[] getAudio(){
        if (savedAudioVersion != operations.getVersion()){
            savedAudioVersion = operations.getVersion();
            boolean[] isOp = operations.getIsOp();
            AudioFormat format = track.getFormat();
            savedAudio = AudioManipulationUtils.applyOps(
                    isOp, track.getDefaultAssetShort(), track.getDefaultAssetByte(), format,
                    operations.getTrimStartSec(), operations.getTrimEndSec(),
                    operations.getVolume(), operations.getPitch()
            );
        }
        return savedAudio;
    }

    public Operations getOperations(){return operations;}

    public double getStartOffsetSec(){return startOffsetSec;}

    public void setStartOffsetSec(double startOffsetSec){this.startOffsetSec = startOffsetSec;}

    public Track getTrack(){ return track; }
}