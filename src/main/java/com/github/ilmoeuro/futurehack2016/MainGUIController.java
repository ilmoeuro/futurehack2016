package com.github.ilmoeuro.futurehack2016;

import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.media.AudioClip;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;

public class MainGUIController implements Initializable {
    private static final int BIAS = 264;
    private static final int JERK_FACTOR = 3;
    private static final int COUNTER_HIGH_LIMIT = 289;
    private static final int COUNTER_LOW_LIMIT = 239;
    private static final List<XYChart.Data> mockData1 = Arrays.asList(
      new XYChart.Data("2016-04-18", 20),
      new XYChart.Data("2016-04-19", 22),
      new XYChart.Data("2016-04-20", 18),
      new XYChart.Data("2016-04-21", 16),
      new XYChart.Data("2016-04-22", 20),
      new XYChart.Data("2016-04-23", 24),
      new XYChart.Data("2016-04-24", 18)
    );
    private static final List<XYChart.Data> mockData2 = Arrays.asList(
      new XYChart.Data("2016-04-18", 50),
      new XYChart.Data("2016-04-19", 50),
      new XYChart.Data("2016-04-20", 51),
      new XYChart.Data("2016-04-21", 25),
      new XYChart.Data("2016-04-22", 20),
      new XYChart.Data("2016-04-23", 22),
      new XYChart.Data("2016-04-24", 19)
    );
    private static final List<XYChart.Data> mockData3 = Arrays.asList(
      new XYChart.Data("2016-04-18", 100),
      new XYChart.Data("2016-04-19", 110),
      new XYChart.Data("2016-04-20", 78),
      new XYChart.Data("2016-04-21", 120),
      new XYChart.Data("2016-04-22", 100),
      new XYChart.Data("2016-04-23", 90),
      new XYChart.Data("2016-04-24", 96)
    );
    private static final List<XYChart.Data> mockData4 = Arrays.asList(
      new XYChart.Data("2016-04-18", 10),
      new XYChart.Data("2016-04-19", 12),
      new XYChart.Data("2016-04-20", 15),
      new XYChart.Data("2016-04-21", 10),
      new XYChart.Data("2016-04-22", 11),
      new XYChart.Data("2016-04-23", 9),
      new XYChart.Data("2016-04-24", 7)
    );
    
    @FXML
    private LineChart<Number, Number> realtimeData;

    @FXML
    private LineChart<String, Number> historyChart1;

    @FXML
    private LineChart<String, Number> historyChart2;

    @FXML
    private TextField serialPortField;

    @FXML
    private Button connectButton;

    @FXML
    private Button disconnectButton;

    @FXML
    private Button enableTTS;

    @FXML
    private TextField ibmUsername;

    @FXML
    private PasswordField ibmPassword;
    
    @FXML
    private Label statusField;

    @FXML
    private Label notice; 

    @FXML
    private Label stats; 
    
    @FXML
    private NumberAxis xAxis;

    private TextToSpeech tts;
    private Instant lastTtsTime = Instant.now();

    private final XYChart.Series acceleration = new XYChart.Series();
    private final XYChart.Series jerk = new XYChart.Series();
    private final XYChart.Series repeats1 = new XYChart.Series();
    private final XYChart.Series mistakes1 = new XYChart.Series();
    private final XYChart.Series repeats2 = new XYChart.Series();
    private final XYChart.Series mistakes2 = new XYChart.Series();
    private final Timeline noticeReset = new Timeline(new KeyFrame(Duration.seconds(3)));
    private final AccelerometerDataSource dataSource = new JsscAccerelometerDataSource();

    private int repeatCounterState = 0;
    private int numRepeats = 0;
    private long lastRepeatTime = 0l;
    private final Map<String, File> fileCache = new ConcurrentHashMap<>();

    private void loadMockData() {
        mistakes1.setName("Mistakes");
        for (XYChart.Data data : mockData1) {
            mistakes1.getData().add(data);
        }

        repeats1.setName("Repeats");
        for (XYChart.Data data : mockData2) {
            repeats1.getData().add(data);
        }

        historyChart1.getData().add(repeats1);
        historyChart1.getData().add(mistakes1);

        mistakes2.setName("Mistakes");
        for (XYChart.Data data : mockData3) {
            mistakes2.getData().add(data);
        }

        repeats2.setName("Repeats");
        for (XYChart.Data data : mockData4) {
            repeats2.getData().add(data);
        }

        historyChart2.getData().add(repeats2);
        historyChart2.getData().add(mistakes2);
    }

    private void showNotification(String notification) {
        notice.setText(notification);
        noticeReset.playFromStart();

        if (tts != null) {
            if (Instant.now().isAfter(lastTtsTime.plusSeconds(1))) {
                 Thread worker = new Thread(() -> {
                        fileCache.computeIfAbsent(notification, n -> {
                            File file = null;
                            try {
                                file = File.createTempFile("futurehack", ".wav");
                                file.deleteOnExit();
                                InputStream stream = tts.synthesize(notification, "audio/wav");
                                FileUtils.copyInputStreamToFile(stream, file);
                            } catch (IOException ex) {
                            }
                            return file;
                        });
                        File file = fileCache.get(notification);
                        AudioClip clip = new AudioClip("file://" + file.getAbsolutePath());
                        Platform.runLater(clip::play);
                });
                worker.start();
                lastTtsTime = Instant.now();
            }
        }
    }
  
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        acceleration.setName("Acceleration");
        noticeReset.setOnFinished((evt) -> {
            notice.setText("");
        });
        jerk.setName("Jolt");
        realtimeData.getData().add(acceleration);
        realtimeData.getData().add(jerk);

        loadMockData();

        enableTTS.setOnAction((event) -> {
            tts = new TextToSpeech();
            tts.setUsernameAndPassword(
                    ibmUsername.getText(),
                    ibmPassword.getText());
        });

        disconnectButton.setOnAction((event) -> {
            dataSource.close();
        });

        connectButton.setOnAction((event) -> {
            dataSource.addErrorHandler(statusField::setText);

            dataSource.addEventHandler((t, a, j) -> {
                Platform.runLater(() -> {
                    xAxis.setLowerBound(t - 10_000);
                    xAxis.setUpperBound(t);
                    acceleration.getData().add(new XYChart.Data<>(t, a - BIAS));
                    jerk.getData().add(new XYChart.Data(t, j * JERK_FACTOR));
                });
            });

            dataSource.addEventHandler((t, a, j) -> {
                if (Math.abs(j) > 10) {
                    Platform.runLater(() -> {
                        showNotification("Too rough");
                    });
                }
            });

            dataSource.addEventHandler((t, a, j) -> {
                Platform.runLater(() -> {
                    if (repeatCounterState == 0 && a > COUNTER_HIGH_LIMIT) {
                        double repeatSpeed = 1000.0 / (t - lastRepeatTime);
                        numRepeats++;
                        repeatCounterState = 1;
                            stats.setText(
                                    String.format("Repeats: %d, Speed: %.3f",
                                            numRepeats,
                                            repeatSpeed));
                        
                        if (repeatSpeed > 2) {
                            showNotification("Too fast");
                        } else if (repeatSpeed < 0.2) {
                            showNotification("Too slow");
                        }
                        
                        lastRepeatTime = t;
                    }
                    if (repeatCounterState == 1 && a < COUNTER_LOW_LIMIT) {
                        repeatCounterState = 0;
                    }
                });
            });

            dataSource.init(serialPortField.getText());
        });
    }    
}
