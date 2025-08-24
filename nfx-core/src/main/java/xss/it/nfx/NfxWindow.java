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

package xss.it.nfx;

import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/**
 * @author XDSSWAR
 * Created on 04/13/2024
 */
public  class NfxWindow extends Stage {

    /**
     * A private member representing an ObjectProperty of type NfxUtil.
     * This ObjectProperty holds a reference to an instance of the NfxUtil class.
     */
    private final ObjectProperty<NfxUtil> nfxUtil;

    /**
     * A private final member representing an EventHandler for WindowEvent.
     * This EventHandler listens for WindowEvents and handles them accordingly.
     */
    private final EventHandler<WindowEvent> LISTENER = windowEvent -> {
        /*
         * TODO : Hope this fix the flicker at shown
         */
        PauseTransition pt = new PauseTransition(Duration.millis(3));
        pt.setOnFinished(event -> {
            ensureNfx();
            if (getTitleBarColor()!=null) {
                getNfxUtil().setTitleBarColor(getTitleBarColor());
            }

            if (getCaptionColor() != null){
                getNfxUtil().setCaptionColor(getCaptionColor());
            }

            getNfxUtil().setCornerPref(getCornerPreference());
            cornerPreferenceProperty().addListener((obs1, o1, pref) ->{
                getNfxUtil().setCornerPref(pref);
                invalidateSpots();
            });

            getNfxUtil().setBorderColor(getWindowBorder());
            windowBorderProperty().addListener((obs1, o1, border) -> {
                getNfxUtil().setBorderColor(border);
                invalidateSpots();
            });

            widthProperty().addListener((obs,o, n) -> {
                update(isMaximized(), isFullScreen());//New update, keep eye
                invalidateSpots();
            });
            heightProperty().addListener((obs,o, n) -> {
                update(isMaximized(), isFullScreen());//New update, keep eye
                invalidateSpots();
            });

            titleBarColorProperty().addListener((obs, o, color) -> {
                if (color != null && getNfxUtil() != null){
                    getNfxUtil().setTitleBarColor(color);
                }
            });

            captionColorProperty().addListener((obs, o, color) -> {
                if (color != null && getNfxUtil() != null){
                    getNfxUtil().setCaptionColor(color);
                }
            });
        });
        pt.play();
    };

    /**
     * Constructs a new NfxWindow instance.
     */
    public NfxWindow() {
        super();
        nfxUtil = new SimpleObjectProperty<>(this, "nfxUtil", null);
        initialize();
    }

    /**
     * Initializes the NfxWindow.
     */
    private void initialize(){
        if (NfxUtil.isWindows()) {
            addEventHandler(WindowEvent.WINDOW_SHOWING, LISTENER);
        }
    }


    /**
     * Returns the property for accessing NfxUtil.
     *
     * @return The property for NfxUtil
     */
    protected final ObjectProperty<NfxUtil> nfxUtilProperty() {
        return nfxUtil;
    }

    /**
     * Returns the NfxUtil instance.
     *
     * @return The NfxUtil instance
     */
    protected NfxUtil getNfxUtil() {
        return nfxUtilProperty().get();
    }


    /**
     * Ensures that the NfxUtil instance is initialized.
     * If not initialized, creates a new instance.
     */
    protected void ensureNfx(){
        if (getNfxUtil() == null){
            nfxUtilProperty().set(new NfxUtil(this));
        }
    }

    /**
     * Resets the nfx
     */
    protected void resetNfx(){
        nfxUtilProperty().set(null);
    }

    /**
     * Property representing the color of the title bar.
     */
    private ObjectProperty<Color> titleBarColor;

    /**
     * Retrieves the property representing the color of the title bar.
     *
     * @return The ObjectProperty representing the color of the title bar.
     */
    public final ObjectProperty<Color> titleBarColorProperty(){
        if (titleBarColor == null){
            titleBarColor = new SimpleObjectProperty<>(this, "titleBarColor");
        }
        return titleBarColor;
    }

    /**
     * Gets the color of the title bar.
     *
     * @return The color of the title bar.
     */
    public final Color getTitleBarColor() {
        return titleBarColorProperty().get();
    }

    /**
     * Sets the color of the title bar.
     *
     * @param titleBarColor The color of the title bar.
     */
    public final void setTitleBarColor(Color titleBarColor) {
        titleBarColorProperty().set(titleBarColor);
    }

    /**
     * Sets the color of the title bar.
     *
     * @param titleBarColor The color of the title bar.
     */
    public final void setTitleBarColor(String titleBarColor) {
        setTitleBarColor(NfxUtil.hexToColor(titleBarColor));
    }


    /**
     * Property representing the color of the window caption.
     */
    private ObjectProperty<Color> captionColor;

    /**
     * Retrieves the property representing the color of the window caption.
     *
     * @return The ObjectProperty representing the color of the window caption.
     */
    public final ObjectProperty<Color> captionColorProperty(){
        if (captionColor == null){
            captionColor = new SimpleObjectProperty<>(this, "captionColor");
        }
        return captionColor;
    }

    /**
     * Gets the color of the window caption.
     *
     * @return The color of the window caption.
     */
    public final Color getCaptionColor() {
        return captionColorProperty().get();
    }

    /**
     * Sets the color of the window caption.
     *
     * @param captionColor The color of the window caption.
     */
    public final void setCaptionColor(Color captionColor) {
        captionColorProperty().set(captionColor);
    }


    /**
     * Sets the color of the window caption.
     *
     * @param htmlColor The color of the window caption.
     */
    public final void setCaptionColor(String htmlColor) {
        setCaptionColor(NfxUtil.hexToColor(htmlColor));
    }

    /**
     * Property representing the corner preference for window corners.
     */
    private ObjectProperty<CornerPreference> cornerPreference;

    /**
     * Returns the ObjectProperty representing the corner preference for window corners.
     * If not already initialized, it creates a new ObjectProperty with a default value of CornerPreference.DEFAULT.
     *
     * @return The ObjectProperty for cornerPreference.
     */
    public final ObjectProperty<CornerPreference> cornerPreferenceProperty() {
        if (cornerPreference == null) {
            cornerPreference = new SimpleObjectProperty<>(this, "cornerPreference", CornerPreference.DEFAULT);
        }
        return cornerPreference;
    }

    /**
     * Gets the corner preference for window corners.
     *
     * @return The corner preference.
     */
    public final CornerPreference getCornerPreference() {
        return cornerPreferenceProperty().get();
    }

    /**
     * Sets the corner preference for window corners.
     *
     * @param cornerPreference The corner preference to set.
     */
    public final void setCornerPreference(CornerPreference cornerPreference) {
        this.cornerPreferenceProperty().set(cornerPreference);
    }

    /**
     * Property representing the window's border color.
     */
    private ObjectProperty<Color> windowBorder;

    /**
     * Returns the ObjectProperty representing the window's border color.
     * If not already initialized, it creates a new ObjectProperty with no default value.
     *
     * @return The ObjectProperty for windowBorder.
     */
    public final ObjectProperty<Color> windowBorderProperty() {
        if (windowBorder == null) {
            windowBorder = new SimpleObjectProperty<>(this, "windowBorder");
        }
        return windowBorder;
    }

    /**
     * Gets the window's border color.
     *
     * @return The window's border color.
     */
    public final Color getWindowBorder() {
        return windowBorderProperty().get();
    }

    /**
     * Sets the window's border color.
     *
     * @param windowBorder The border color to set.
     */
    public final void setWindowBorder(Color windowBorder) {
        this.windowBorderProperty().set(windowBorder);
    }

    /**
     * Invalidates the spots, triggering a refresh or update in the spot-related components.
     * This method should be overridden in subclasses to implement specific invalidation logic.
     */
    protected void invalidateSpots(){}

    /**
     * Updates the window state based on the provided parameters.
     *
     * @param max  Whether the window should be maximized.
     * @param full Whether the window should be in full-screen mode.
     */
    protected void update(boolean max, boolean full){}
}
