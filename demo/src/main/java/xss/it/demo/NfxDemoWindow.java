/*
 * Copyright © 2025. XTREME SOFTWARE SOLUTIONS
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

import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.SVGPath;
import xss.it.nfx.NfxStage;
import xss.it.nfx.WindowState;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * nfx
 * <p>
 * Description:
 * This class is part of the xss.it.demo package.
 *
 * <p>
 *
 * @author XDSSWAR
 * @version 1.0
 * @since August 13, 2025
 * <p>
 * Created on 08/13/2025 at 20:13
 * <p>
 * Demo window using {@code NfxStage}.
 * <p>
 * Provides a fixed 40 DIP title bar height and hooks optional initialization.
 * If used as an FXML controller, JavaFX will call {@link #initialize(URL, ResourceBundle)}.
 * Avoid calling the no-arg initialize from the constructor when fields are injected via FXML.
 */
public class NfxDemoWindow extends NfxStage implements Initializable {
    @FXML
    private Button closeBtn;  /* Close control in the custom title bar. */

    @FXML
    private Button maxBtn;    /* Maximize/restore control in the custom title bar. */

    @FXML
    private SVGPath maxShape; /* Graphic for the maximize/restore button (styled via CSS). */

    @FXML
    private Button minBtn;    /* Minimize control in the custom title bar. */

    @FXML
    private ImageView iconView; /* For the icon */

    @FXML
    private Label title; //Title

    /**
     * Creates the demo window and runs local initialization for non-FXML usage.
     * If this class is used as an FXML controller, prefer relying on
     * {@link #initialize(URL, ResourceBundle)} instead of the no-arg initialize.
     */
    public NfxDemoWindow() {
        super();
        try {
            //Set the icon
            getIcons().add(
                    new Image(Assets.load("/icon.png").toExternalForm())
            );

            //Load fxml
            Parent parent = Assets.load("/mfx-window.fxml", this);
            Scene scene = new Scene(parent);
            setScene(scene);
            initialize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Local setup for non-FXML use cases.
     * Put programmatic UI wiring and event handlers here.
     * Do not assume FXML-injected fields are available in this method.
     */
    private void initialize(){
        setTitle("NfxStage Demo");

    }

    /**
     * Returns the title bar height in DIP (logical pixels) for hit testing and layout.
     *
     * @return 40 DIP title bar height
     */
    @Override
    protected double getTitleBarHeight() {
        return 40;
    }

    /**
     * Called by the JavaFX runtime when this class is used as an FXML controller.
     * Perform initialization that relies on FXML-injected fields here.
     *
     * @param location  location of the FXML, if available
     * @param resources resource bundle, if available
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        getIcons().addListener((ListChangeListener<? super Image>) observable -> {
            if (!getIcons().isEmpty()){
                iconView.setImage(getIcons().getFirst());
            }
        });
        titleProperty().addListener(observable -> {
            title.setText(getTitle());
        });


        setCloseControl(closeBtn);
        setMaxControl(maxBtn);
        setMinControl(minBtn);

        handleMaxStateChangeShape(getWindowState());
        windowStateProperty().addListener((obs, o, state)
                -> handleMaxStateChangeShape(state));
    }

    /**
     * Updates the maximize/restore button graphic based on the current window state.
     * <p>
     * When the window is {@code MAXIMIZED}, the SVG path is set to {@code REST_SHAPE}
     * (restore glyph); otherwise it is set to {@code MAX_SHAPE} (maximize glyph).
     * Call on the JavaFX Application Thread.
     *
     * @param state the current {@link WindowState}
     */
    private void  handleMaxStateChangeShape(WindowState state){
        if (Objects.equals(state, WindowState.MAXIMIZED)){
            maxShape.setContent(REST_SHAPE);
        }
        else {
            maxShape.setContent(MAX_SHAPE);
        }
    }

    /**
     * SVG path data for the "Minimize" button icon.
     */
    public static final String MIN_SHAPE = "M1 7L1 8L14 8L14 7Z";

    /**
     * SVG path data for the "Maximize" button icon.
     */
    public static final String MAX_SHAPE = "M2.5 2 A 0.50005 0.50005 0 0 0 2 2.5L2 13.5 A 0.50005 0.50005 0 0 0 2.5 14L13.5 14 A 0.50005 0.50005 0 0 0 14 13.5L14 2.5 A 0.50005 0.50005 0 0 0 13.5 2L2.5 2 z M 3 3L13 3L13 13L3 13L3 3 z";

    /**
     * SVG path data for the "Restore" button icon (used when window is maximized).
     */
    public static final String REST_SHAPE = "M4.5 2 A 0.50005 0.50005 0 0 0 4 2.5L4 4L2.5 4 A 0.50005 0.50005 0 0 0 2 4.5L2 13.5 A 0.50005 0.50005 0 0 0 2.5 14L11.5 14 A 0.50005 0.50005 0 0 0 12 13.5L12 12L13.5 12 A 0.50005 0.50005 0 0 0 14 11.5L14 2.5 A 0.50005 0.50005 0 0 0 13.5 2L4.5 2 z M 5 3L13 3L13 11L12 11L12 4.5 A 0.50005 0.50005 0 0 0 11.5 4L5 4L5 3 z M 3 5L11 5L11 13L3 13L3 5 z";

    /**
     * SVG path data for the "Close" button icon.
     */
    public static final String CLOSE_SHAPE = "M3.726563 3.023438L3.023438 3.726563L7.292969 8L3.023438 12.269531L3.726563 12.980469L8 8.707031L12.269531 12.980469L12.980469 12.269531L8.707031 8L12.980469 3.726563L12.269531 3.023438L8 7.292969Z";

}
