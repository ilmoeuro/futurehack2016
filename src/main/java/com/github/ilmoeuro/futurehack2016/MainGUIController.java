package com.github.ilmoeuro.futurehack2016;

import java.net.URL;
import java.util.ResourceBundle;
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
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class MainGUIController implements Initializable {
    private static final int BIAS = 264;
    private static final int JERK_FACTOR = 3;
    
    @FXML
    private LineChart<Number, Number> realtimeData;

    @FXML
    private TextField serialPortField;

    @FXML
    private Button connectButton;

    @FXML
    private Button disconnectButton;
    
    @FXML
    private Label statusField;

    @FXML
    private Label notice; 
    
    @FXML
    private NumberAxis xAxis;

    private final XYChart.Series acceleration = new XYChart.Series();
    private final XYChart.Series jerk = new XYChart.Series();
    private final Timeline noticeReset = new Timeline(new KeyFrame(Duration.seconds(10)));
    private final AccelerometerDataSource dataSource = new JsscAccerelometerDataSource();
  
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        acceleration.setName("Acceleration");
        noticeReset.setOnFinished((evt) -> {
            notice.setText("");
        });
        jerk.setName("Jolt");
        realtimeData.getData().add(acceleration);
        realtimeData.getData().add(jerk);

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
                if (j > 10) {
                    Platform.runLater(() -> {
                        notice.setText("Too rough");
                        noticeReset.playFromStart();
                    });
                }
            });
            dataSource.init(serialPortField.getText());
        });
    }    
}
