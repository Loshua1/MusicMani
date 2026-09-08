package com.example.music;

import com.example.music.data.Instance;
import com.example.music.data.Project;
import com.example.music.data.Track;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Slider;
import javafx.scene.control.Separator;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.converter.NumberStringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;

public class MainController {
    private Project project;
    private String projectName;
    private AudioPlayback audioPlayback;
    private final int pixelsPerSec = 20;
    private Polygon timeTrackerHandle;
    private Line timeTrackerLine;
    private double dragMouseLineStartX;
    private final double timeTrackerHandleOffset = -7.5;
    //new canvas every 30 sec
    private final int chunkDurationSec = 30;

    @FXML
    private VBox allTracksMenu;
    @FXML
    private HBox addTrackMenu;
    @FXML
    private Button addTrackMenuButton;
    @FXML
    private VBox allTracksVisual;
    @FXML
    private ScrollPane leftScrollPane;
    @FXML
    private ScrollPane centerScrollPane;
    @FXML
    private ScrollPane timelineScrollPane;
    @FXML
    private StackPane centerStackPane;
    @FXML
    private StackPane topStackPane;

    @FXML
    private void initialize(){
        leftScrollPane.vvalueProperty().bindBidirectional(centerScrollPane.vvalueProperty());
        timelineScrollPane.hvalueProperty().bindBidirectional(centerScrollPane.hvalueProperty());
        drawTimeline();
        drawTimelineTracker();
    }

    @FXML
    protected void onPlayButtonClick() {
        if (!audioPlayback.isPlaying()){
            try{
                audioPlayback.updateInstances(project.getInstances());
                audioPlayback.play();
            } catch(Exception e) {
                System.out.println("playing audio failed: " + e);
            }
        }
    }
    @FXML
    protected void onPauseButtonClick() {
        audioPlayback.stop();
    }

    @FXML
    protected void onExportButtonClick() {
        if (projectName == null || projectName.isEmpty()) {
            setProjectName(true);
        } else {
            AudioFormat format = project.getFormat();
        //TODO: make the directory chooser have a name chooser attached or something
            int len = 0;
            for (Instance instance : project.getInstances()) {
                len += instance.getAudio().length;
            }
            short[] overallMixOutput = new short[len];
            int currFrame = 0;
            int frames = 1024;
            short[] mixOutput = new short[frames];

            while (currFrame < len) {
                Arrays.fill(mixOutput, (short) 0);

                for (Instance instance : project.getInstances()) {
                    short[] instAudio = instance.getAudio();
                    long instanceStartFrame = (long) (instance.getStartOffsetSec() * format.getSampleRate());
                    for (int j = 0; j < frames; j++) {
                        long overallFrame = currFrame + j;
                        long frameOfCurrInstance = overallFrame - instanceStartFrame;
                        if (frameOfCurrInstance >= 0 && frameOfCurrInstance < instAudio.length) {
                            int mixed = mixOutput[j] + instAudio[(int) frameOfCurrInstance];
                            mixOutput[j] = AudioManipulationUtils.toShort(mixed);
                        }
                    }
                }

                int lenCopy = Math.min(len - currFrame, frames);
                System.arraycopy(mixOutput, 0, overallMixOutput, currFrame, lenCopy);

                //advances time
                currFrame += frames;

            }

            byte[] mixOutputByte = AudioManipulationUtils.toByteArray(overallMixOutput);
            ByteArrayInputStream bais = new ByteArrayInputStream(mixOutputByte);
            AudioInputStream ais = new AudioInputStream(bais, format, mixOutputByte.length);

            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose export location");
            directoryChooser.getInitialDirectory();
            File selectedDirectory = directoryChooser.showDialog(addTrackMenuButton.getScene().getWindow());
            if (selectedDirectory != null) {
                File mixedFile = new File(selectedDirectory.getAbsolutePath() + "\\" + projectName + ".wav");

                try {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, mixedFile);
                    ais.close();
                } catch(Exception e){
                    System.out.println("audio export failed: " + e);
                }
            }
        }
    }

    @FXML
    protected void onSetProjectNameButtonClick(){
        setProjectName(false);
    }

    private void setProjectName(boolean isExporting) {
        StackPane prompt = new StackPane();
        prompt.setTranslateX(0);
        prompt.setTranslateY(30);
        prompt.setAlignment(Pos.CENTER);

        Rectangle rectBorder = new Rectangle(305, 105);
        rectBorder.setFill(Color.BLACK);
        Rectangle rect = new Rectangle(300, 100);
        rect.setFill(Color.WHITE);

        String text;
        if (isExporting) text = "Name Before Exporting";
        else text = "Name Project";
        Text question = new Text(text);
        question.setFont(Font.font("Arimo", 20));
        question.setTextAlignment(TextAlignment.CENTER);
        question.setTranslateY(-25);

        TextField userProject = new TextField();
        userProject.setPromptText("Project");
        userProject.setPrefSize(200, 20);
        userProject.setMaxSize(200, 20);

        Button save = new Button("Save");
        save.setTranslateX(-50); save.setTranslateY(25);
        save.setOnMouseClicked(event -> {
            if (!userProject.getText().isEmpty()) {
                projectName = userProject.getText();
                System.out.println(projectName);
            }
            centerStackPane.getChildren().removeLast();
            onExportButtonClick();
        });

        Button cancel = new Button("Cancel");
        cancel.setTranslateX(50); cancel.setTranslateY(25);
        cancel.setOnMouseClicked(event -> centerStackPane.getChildren().removeLast());

        prompt.getChildren().addAll(rectBorder, rect, question, userProject, save, cancel);
        centerStackPane.getChildren().add(prompt);
    }

    @FXML
    protected void onAddTrackMenuButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Add Track");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.aiff", "*.au"));
        File selectedFile = fileChooser.showOpenDialog(addTrackMenuButton.getScene().getWindow());
        if (selectedFile != null){
            project.addTrack(selectedFile);
            createTrack(selectedFile);
        }
    }

    private void createTrack(File file){
        int index = allTracksMenu.getChildren().indexOf(addTrackMenu);
        Track track = project.getTracks().getLast();
        final int trackIndex = project.getTracks().size() - 1;
        Button deleteTrack = new Button("", new FontIcon("fas-times"));
        deleteTrack.setPrefSize(30, 20);
        deleteTrack.setOnMouseClicked(event -> onDeleteTrackButton(trackIndex));
        //adds track menu
        HBox name = new HBox(deleteTrack, new Text(file.getName()));
        name.setAlignment(Pos.CENTER);
        allTracksMenu.getChildren().add(index, new Separator());
        allTracksMenu.getChildren().add(index, addNewTrackMenu(track));
        allTracksMenu.getChildren().add(index, name);

        //adds track visual
        HBox blank = new HBox(new Text());
        allTracksVisual.getChildren().add(index, new Separator());
        allTracksVisual.getChildren().add(index, addTrackVisual());
        allTracksVisual.getChildren().add(index, blank);

        drawTimeline();
    }

    private VBox addNewTrackMenu(Track track){
        VBox newMenu = new VBox();

        HBox buttonMenu = new HBox();
        buttonMenu.setAlignment(Pos.CENTER);
        Button mute = new Button("Mute", new FontIcon("fas-volume-mute"));
        mute.setOnMouseClicked(event -> track.flipEnabled());
        mute.setPrefSize(50, 20);
        buttonMenu.getChildren().add(mute);

        VBox sliderMenu = new VBox();
        newMenu.setAlignment(Pos.CENTER);
        Text volumeTxt = new Text("Volume");
        Text pitchTxt = new Text("Pitch");

        Slider volumeSlider = new Slider(-50, 50, 0);
        setUpSlider(volumeSlider);
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                track.getLastInstance().getOperations().editVolume((double)newValue)

        );
        TextField volumeField = new TextField();
        volumeField.textProperty().bindBidirectional(volumeSlider.valueProperty(), new NumberStringConverter());

        Slider pitchSlider = new Slider(-50, 50, 0);
        setUpSlider(pitchSlider);
        pitchSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                track.getLastInstance().getOperations().editPitch((double)newValue)

        );

        sliderMenu.getChildren().addAll(volumeTxt, volumeSlider, pitchTxt, pitchSlider);
        newMenu.getChildren().addAll(buttonMenu, new Separator(), sliderMenu);
        return newMenu;
    }

    private void setUpSlider(Slider slider) {
        slider.setOrientation(Orientation.HORIZONTAL);
        slider.setMajorTickUnit(10);
        slider.setMinorTickCount(5);
        slider.setSnapToTicks(true);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
    }

    private HBox addTrackVisual(){
        HBox newTrack = new HBox();
        Instance currInstance = project.getInstances().getLast();
        short[] instanceAudio = currInstance.getAudio();
        int numChannels = project.getTracks().getLast().getNumChannels();
        float sampleRate = project.getFormat().getSampleRate();

        long samplesPerChunk = (long) (chunkDurationSec * numChannels * sampleRate);
        int numChunks = (int) Math.ceil(instanceAudio.length / (double) samplesPerChunk);

        for (int i = 0; i < numChunks; i++){
            int start = (int)(i * samplesPerChunk);
            int end = (int) Math.min(start + samplesPerChunk, instanceAudio.length);
            short[] currInstanceAudio = Arrays.copyOfRange(instanceAudio, start, end);
            newTrack.getChildren().add(drawChunk(currInstanceAudio, numChannels, sampleRate, samplesPerChunk));
        }
        return newTrack;
    }

    private Canvas drawChunk(short[] currInstanceAudio, int numChannels, float sampleRate, long samplesPerChunk){
        Canvas canvas = new Canvas();

        double audioDuration = currInstanceAudio.length / (numChannels * sampleRate);
        double width = ((double)samplesPerChunk) / (numChannels * sampleRate) * pixelsPerSec;
        canvas.setHeight(140);
        canvas.setWidth(width);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);

        for (int i = 0; i < width; i++) {
            int currSample = (int)(i * ((sampleRate * numChannels) / pixelsPerSec));
            int endSample = Math.min(currSample + pixelsPerSec, currInstanceAudio.length);

            short max = 0;
            short min = 0;
            for (int j = currSample; j < endSample; j++){
                if (currInstanceAudio[j] > max) max = currInstanceAudio[j];
                if (currInstanceAudio[j] < min) min = currInstanceAudio[j];
            }

            double y1 = 70 - ((max / (double) Short.MAX_VALUE) * 70);
            double y2 = 70 - ((min / (double) Short.MAX_VALUE) * 70);

            gc.strokeLine(i, y1, i, y2);
        }

        return canvas;
    }

    private void drawTimeline(){
        HBox timeline = new HBox();

        double seconds = 600;
        if (project != null && !project.getTracks().isEmpty()) {
            seconds = Math.ceil(project.getLongestSec());
        }

        int numChunks = (int) Math.ceil(seconds / chunkDurationSec);
        double width = chunkDurationSec * pixelsPerSec;

        int overallSec = 0;

        for (int chunk = 0; chunk < numChunks; chunk++) {
            Canvas timelineChunk = new Canvas(width, 30);
            GraphicsContext gc = timelineChunk.getGraphicsContext2D();

            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeLine(0, 15, 0, 30);
            gc.fillText(getSecondsText(overallSec), -7.5, 12);
            gc.setLineWidth(1);

            for (int localSec = 0; localSec < chunkDurationSec; localSec++){
                overallSec++;
                double x = localSec * pixelsPerSec;
                gc.strokeLine(x, 20, x, 30);
            }
            gc.setLineWidth(2);
            gc.strokeLine(width, 15, width, 30);
            gc.fillText(getMinutesText(overallSec), width - 7.5, 12);

            timeline.getChildren().addLast(timelineChunk);
        }

        if (!topStackPane.getChildren().isEmpty()) {
            for (int i = 0; i < topStackPane.getChildren().size(); i++) {
                topStackPane.getChildren().removeFirst();
            }
        }
        topStackPane.getChildren().addFirst(timeline);
    }

    private void drawTimelineTracker(){
        timeTrackerLine = new Line();
        timeTrackerLine.setFill(Color.RED);
        timeTrackerLine.setStrokeWidth(1);
        timeTrackerLine.setMouseTransparent(true);
        timeTrackerLine.setStartY(0);
        timeTrackerLine.endYProperty().bind(allTracksVisual.heightProperty());
        centerStackPane.getChildren().add(timeTrackerLine);

        timeTrackerHandle = new Polygon(-10, 0, 10, 0, 0, 14);
        timeTrackerHandle.setFill(Color.RED);
        timeTrackerHandle.setTranslateX(timeTrackerHandleOffset);
        timeTrackerHandle.setTranslateY(15);
        topStackPane.getChildren().add(timeTrackerHandle);

        timeTrackerHandle.setOnMouseEntered(e -> timeTrackerHandle.setFill(Color.color(1, 0, 0, .5)));
        timeTrackerHandle.setOnMouseExited(e -> timeTrackerHandle.setFill(Color.color(1, 0, 0, 1)));
        timeTrackerHandle.setOnMousePressed(e -> {
            if (dragMouseLineStartX == 0)
                dragMouseLineStartX = e.getSceneX();
        });
        timeTrackerHandle.setOnMouseDragged(e -> {
            double diff = e.getSceneX() - dragMouseLineStartX;
            double newX = Math.max(0, diff);
            timeTrackerLine.setTranslateX(newX);
            timeTrackerHandle.setTranslateX(newX + timeTrackerHandleOffset);
            audioPlayback.stop();
        });
        timeTrackerHandle.setOnMouseReleased(e -> {
            double sec = timeTrackerHandle.getTranslateX() / pixelsPerSec;
            if (audioPlayback != null) audioPlayback.setSec(sec);
        });
        AnimationTimer timelineTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (audioPlayback != null && audioPlayback.isPlaying()) {
                    double x = audioPlayback.getSec() * pixelsPerSec;
                    dragMouseLineStartX = x;
                    timeTrackerLine.setTranslateX(x);
                    timeTrackerHandle.setTranslateX(x);
                    timeTrackerHandle.setTranslateX(x + timeTrackerHandleOffset);
                }
            }
        };
        timelineTimer.start();
    }

    private void onDeleteTrackButton(int index){
        System.out.println("indexxx: " + index);
        StackPane prompt = new StackPane();
        prompt.setTranslateX(-750);
        prompt.setTranslateY(30);
        prompt.setAlignment(Pos.CENTER);

        Rectangle rectBorder = new Rectangle(305, 105);
        rectBorder.setFill(Color.BLACK);
        Rectangle rect = new Rectangle(300, 100);
        rect.setFill(Color.WHITE);

        Text question = new Text("Are you sure you would\nlike to delete this track?");
        question.setFont(Font.font("Arimo", 20));
        question.setTextAlignment(TextAlignment.CENTER);
        question.setTranslateY(-25);

        Button del = new Button("Delete");
        del.setTranslateX(-50); del.setTranslateY(25);
        del.setOnMouseClicked(event -> {
            centerStackPane.getChildren().removeLast();
            project.deleteTrack(index);
            for (int i = 0; i < 3; i++) {
                allTracksMenu.getChildren().remove(index * 3);
                allTracksVisual.getChildren().remove(index * 3);
            }
            drawTimeline();
        });

        Button cancel = new Button("Cancel");
        cancel.setTranslateX(50); cancel.setTranslateY(25);
        cancel.setOnMouseClicked(event -> centerStackPane.getChildren().removeLast());

        prompt.getChildren().addAll(rectBorder, rect, question, del, cancel);
        centerStackPane.getChildren().add(prompt);
    }

    private String getMinutesText(int seconds){
        int min = seconds / 60;
        return String.format("%d:    ", min);
    }

    private String getSecondsText(int seconds){
        int sec = seconds % 60;
        return String.format("  :%02d", sec);
    }

    public void setProject(Project project){
        this.project = project;
    }
    public void setAudioPlayback(AudioPlayback audioPlayback){
        this.audioPlayback = audioPlayback;
    }

}
