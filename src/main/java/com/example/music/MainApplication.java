package com.example.music;

import com.example.music.data.Project;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.util.ArrayList;

public class MainApplication extends Application {

    private final Project project = new Project();
    private final AudioPlayback audioPlayback = new AudioPlayback(
            new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    44100,
                    16,
                    2,
                    4,
                    44100,
                    false
            ),
            new ArrayList<>()
    );

    public MainApplication() throws LineUnavailableException {
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1920, 1080);
        stage.setTitle("MusicDAW");
        stage.setScene(scene);
        stage.show();
        ((MainController) fxmlLoader.getController()).setProject(project);
        ((MainController) fxmlLoader.getController()).setAudioPlayback(audioPlayback);

        stage.setOnCloseRequest(windowEvent -> audioPlayback.stop());
    }
}
