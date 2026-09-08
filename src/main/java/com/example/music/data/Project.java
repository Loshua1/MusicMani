package com.example.music.data;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Project {
    private final List<Track> tracks = new ArrayList<>();
    private double longestSec;

    public Project() {}

    public void addTrack(File file){
        tracks.add(new Track(file.getPath()));
        Track currTrack = tracks.getLast();
        double currSec = currTrack.getDefaultAssetShort().length /
                ((double) currTrack.getNumChannels() * currTrack.getFormat().getSampleRate());
        longestSec = Math.max(longestSec, currSec);
    }

    public void deleteTrack(int index){
        tracks.remove(index);
    }

    public AudioFormat getFormat() {
        return tracks.getFirst().getFormat();
    }

    public ArrayList<Instance> getInstances() {
        ArrayList<Instance> instances = new ArrayList<>();
        for (Track track : tracks) {
            if (track.isEnabled()) {
                instances.addAll(track.getInstances());
            }
        }
        return instances;
    }

    public List<Track> getTracks() {return tracks;}

    public void setLongestSec(double longestSec){
        this.longestSec = longestSec;
    }

    public double getLongestSec() { return longestSec; }
}
