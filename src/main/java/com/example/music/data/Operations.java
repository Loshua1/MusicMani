package com.example.music.data;

import java.util.Arrays;

public class Operations {
    private int version = 0;
    // determines whether each op is used or not
    // [trim,volume,pitch]
    private final boolean[] isOp = new boolean[3];
    private double trimStartSec;
    private double trimEndSec;
    private double volume;
    private double pitch;

    public void trim(double trimStartSec, double trimEndSec) {
        isOp[0] = true;
        this.trimStartSec = trimStartSec;
        this.trimEndSec = trimEndSec;
        version++;
    }
    public void editVolume(double volume) {
        isOp[1] = true;
        this.volume = volume;
        version++;
    }
    public void editPitch(double pitch) {
        isOp[2] = true;
        this.pitch = pitch;
        version++;
    }

    public int getVersion(){
        return version;
    }

    public boolean[] getIsOp(){
        return Arrays.copyOf(isOp, isOp.length);
    }

    public double getTrimStartSec(){return trimStartSec;}
    public double getTrimEndSec(){return trimEndSec;}
    public double getVolume(){return volume;}
    public double getPitch(){return pitch;}
}
