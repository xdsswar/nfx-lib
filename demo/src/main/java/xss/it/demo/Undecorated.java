/*
 * Copyright © 2024. XTREME SOFTWARE SOLUTIONS
 *
 * All rights reserved. Unauthorized use, reproduction, or distribution
 * of this software or any portion of it is strictly prohibited and may
 * result in severe civil and criminal penalties. This code is the sole
 * proprietary of XTREME SOFTWARE SOLUTIONS.
 *
 * Commercialization, redistribution, and use without explicit permission
 * from XTREME SOFTWARE SOLUTIONS, are expressly forbidden.
 */

package xss.it.demo;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * @author XDSSWAR
 * Created on 04/17/2024
 */
public class Undecorated extends Application {

    /**
     * The entry point of the Java application.
     * This method calls the launch method to start a JavaFX application.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * This method is called after the application has been launched.
     * Override this method to create and set up the primary stage of the application.
     *
     * @param stage The primary stage for this application, onto which
     *              the application scene can be set.
     */
    @Override
    public void start(Stage stage) {
        Window window = new Window();

        window.windowStateProperty().addListener((observableValue, state, t1) -> {
            System.out.println(t1);
        });
        VBox box = new VBox();
        box.setAlignment(Pos.TOP_CENTER);


        Button b = new Button("Test");
        box.getChildren().add(b);

        window.setWindowBackground(Color.RED);

        box.setStyle("-fx-background-color: #1d7340");
        box.setPrefSize(600, 400);
        Scene scene = new Scene(box);
        window.setScene(scene);

        window.show();
    }

    /**
     * The initialization method for the application.
     * This method is called immediately after the application class is loaded and
     * constructed. An application can override this method to perform initialization
     * tasks before the application is shown.
     *
     * @throws Exception if an error occurs during initialization.
     */
    @Override
    public void init() throws Exception {
        super.init();
    }

    /**
     * This method is called when the application should stop, and provides a
     * convenient place to prepare for application exit and destroy resources.
     *
     * @throws Exception if an error occurs during stopping the application.
     */
    @Override
    public void stop() throws Exception {
        super.stop();
    }
}
