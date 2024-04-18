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

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.shape.SVGPath;
import xss.it.nfx.AbstractNfxUndecoratedWindow;
import xss.it.nfx.HitSpot;
import xss.it.nfx.WindowState;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author XDSSWAR
 * Created on 04/18/2024
 */
public class MenuWindow extends AbstractNfxUndecoratedWindow implements Initializable {
    @FXML
    private Button closeBtn, maxBtn, minBtn;

    @FXML
    private MenuBar menuBar;



    /**
     * The height of the title bar.
     */
    private static final int TITLE_BAR_HEIGHT = 30;


    public MenuWindow() {
        try {
            Parent parent = Assets.load("/menu-window.fxml", this);
            Scene scene = new Scene(parent);
            setScene(scene);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        closeBtn.setOnAction(event -> close());

        maxBtn.setOnAction(event -> {
            if (getWindowState().equals(WindowState.MAXIMIZED)){
                setWindowState(WindowState.NORMAL);
            }
            else {
                setWindowState(WindowState.MAXIMIZED);
            }
        });

        minBtn.setOnAction(event -> setWindowState(WindowState.MINIMIZED));

        handelState(getWindowState());
        windowStateProperty().addListener((obs, o, state) -> handelState(state));
    }

    /**
     * Handles the state of the window.
     *
     * @param state The state of the window (e.g., MAXIMIZED, NORMAL).
     */
    private void handelState(WindowState state){
        if (maxBtn.getGraphic() instanceof SVGPath path) {
            if (state.equals(WindowState.MAXIMIZED)) {
                path.setContent(REST_SHAPE);
            } else if (state.equals(WindowState.NORMAL)) {
                path.setContent(MAX_SHAPE);
            }
        }
    }

    /**
     * Retrieves the list of hit spots in the window.
     *
     * @return The list of hit spots.
     */
    @Override
    public List<HitSpot> getHitSpots() {
        HitSpot minimizeHitSpot = HitSpot.builder()
                .control(minBtn)
                .minimize(true)
                .build();

        minimizeHitSpot.hoveredProperty().addListener((obs, o, hovered) -> {
            if (hovered){
                minimizeHitSpot.getControl().getStyleClass().add("hit-hovered");
            }
            else {
                minimizeHitSpot.getControl().getStyleClass().remove("hit-hovered");
            }
        });

        HitSpot maximizeHitSpot = HitSpot.builder()
                .control(maxBtn)
                .maximize(true)
                .build();

        maximizeHitSpot.hoveredProperty().addListener((obs, o, hovered) -> {
            if (hovered){
                maximizeHitSpot.getControl().getStyleClass().add("hit-hovered");
            }
            else {
                maximizeHitSpot.getControl().getStyleClass().remove("hit-hovered");
            }
        });

        HitSpot closeHitSpot = HitSpot.builder()
                .control(closeBtn)
                .close(true)
                .build();

        closeHitSpot.hoveredProperty().addListener((obs, o, hovered) -> {
            if (hovered){
                closeHitSpot.getControl().getStyleClass().add("hit-close-btn");
                closeBtn.getGraphic().getStyleClass().add("shape-close-hovered");
            }
            else {
                closeHitSpot.getControl().getStyleClass().remove("hit-close-btn");
                closeBtn.getGraphic().getStyleClass().remove("shape-close-hovered");
            }
        });


        HitSpot bar = HitSpot.builder()
                .control(menuBar)
                .build();

        return List.of(minimizeHitSpot, maximizeHitSpot, closeHitSpot, bar);
    }


    /**
     * Retrieves the height of the title bar.
     *
     * @return The height of the title bar.
     */
    @Override
    public double getTitleBarHeight() {
        return TITLE_BAR_HEIGHT;
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
