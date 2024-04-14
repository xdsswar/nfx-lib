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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * @author XDSSWAR
 * Created on 04/13/2024
 */
public final class NfxWindow extends Stage {
    /**
     * The NfxUtil instance for handling native window operations.
     */
    private NfxUtil nfxUtil = null;

    /**
     * Constructs a new NfxWindow instance.
     */
    public NfxWindow() {
        super();
        initialize();
    }

    /**
     * Initializes the NfxWindow.
     */
    private void initialize(){
        setOnShown(windowEvent -> {
            ensureNfx();
            if (getTitleBarColor()!=null) {
                nfxUtil.setTitleBarColor(getTitleBarColor());
            }

            if (getCaptionColor() != null){
                nfxUtil.setCaptionColor(getCaptionColor());
            }
        });


        titleBarColorProperty().addListener((obs, o, color) -> {
            if (color != null && nfxUtil != null){
                nfxUtil.setTitleBarColor(color);
            }
        });

        captionColorProperty().addListener((obs, o, color) -> {
            if (color != null && nfxUtil != null){
                nfxUtil.setCaptionColor(color);
            }
        });
    }


    /**
     * Ensures that the NfxUtil instance is initialized.
     * If not initialized, creates a new instance.
     */
    private void ensureNfx(){
        if (nfxUtil == null){
            nfxUtil = new NfxUtil(this);
        }
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
    public ObjectProperty<Color> titleBarColorProperty(){
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
    public Color getTitleBarColor() {
        return titleBarColorProperty().get();
    }

    /**
     * Sets the color of the title bar.
     *
     * @param titleBarColor The color of the title bar.
     */
    public void setTitleBarColor(Color titleBarColor) {
        titleBarColorProperty().set(titleBarColor);
    }

    /**
     * Sets the color of the title bar.
     *
     * @param titleBarColor The color of the title bar.
     */
    public void setTitleBarColor(String titleBarColor) {
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
    public ObjectProperty<Color> captionColorProperty(){
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
    public Color getCaptionColor() {
        return captionColorProperty().get();
    }

    /**
     * Sets the color of the window caption.
     *
     * @param captionColor The color of the window caption.
     */
    public void setCaptionColor(Color captionColor) {
        captionColorProperty().set(captionColor);
    }


    /**
     * Sets the color of the window caption.
     *
     * @param htmlColor The color of the window caption.
     */
    public void setCaptionColor(String htmlColor) {
        setCaptionColor(NfxUtil.hexToColor(htmlColor));
    }


}
