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
import xss.it.nfx.NfxWindow;

import java.util.Random;

/**
 * @author XDSSWAR
 * Created on 04/13/2024
 */
public class Demo extends Application {

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
        NfxWindow window = new NfxWindow();
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);

        Button button= new Button("Random Color");
        box.getChildren().add(button);

        box.setPrefSize(1000, 600);
        window.setTitle("Custom TitleBar");
        window.setScene(new Scene(box));
        window.show();

        button.setOnAction(actionEvent -> {
            var c = generateRandomColor();
            box.setStyle("-fx-background-color: "+ colorToHex(c)+";");
            window.setTitleBarColor(c);

        });



    }

    public static Color generateRandomColor() {
        Random random = new Random();
        double red = random.nextDouble();
        double green = random.nextDouble();
        double blue = random.nextDouble();
        return new Color(red, green, blue, 1.0); // Alpha is set to 1 for full opacity
    }

    private static String colorToHex(Color color) {
        String red = Integer.toHexString((int) (color.getRed() * 255));
        String green = Integer.toHexString((int) (color.getGreen() * 255));
        String blue = Integer.toHexString((int) (color.getBlue() * 255));

        red = padWithZeroes(red);
        green = padWithZeroes(green);
        blue = padWithZeroes(blue);
        return "#" + red + green + blue;
    }

    private static String padWithZeroes(String hex) {
        return hex.length() == 1 ? "0" + hex : hex;
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
        //System.load("E:\\Development\\java\\nfx\\nfx-core\\src\\native\\cmake-build-release-visual-studio\\nfx-core-1.0.0.dll");
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
