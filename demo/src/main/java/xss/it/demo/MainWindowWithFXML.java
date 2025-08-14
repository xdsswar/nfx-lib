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
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import xss.it.nfx.NfxWindow;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * @author XDSSWAR
 * Created on 04/17/2024
 */
public class MainWindowWithFXML extends NfxWindow implements Initializable {
    @FXML
    private Button openCustomDialogBtn, openUndecoratedBtn,
            openCustomDialogHiddenFromTaskBarBtn, menuFxWindowBtn, updateDemoBtn;

    /**
     * Constructs a new instance of MainWindowWithFXML.
     */
    public MainWindowWithFXML() {
        setTitleBarColor("#464646");
    }

    /**
     * Initializes the controller.
     *
     * @param url            The location used to resolve relative paths for the root object, or null if the location is not known.
     * @param resourceBundle The resources used to localize the root object, or null if the root object was not localized.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        openUndecoratedBtn.setOnAction(event -> {
            UndecoratedExample undecoratedExample = new UndecoratedExample();
            undecoratedExample.setTitle("Undecorated NfxWindow Demo");
            undecoratedExample.getIcons().add(
                    new Image(Assets.load("/icon.png").toExternalForm())
            );
            undecoratedExample.show();
        });

        openCustomDialogBtn.setOnAction(event -> {
            DialogExample example = new DialogExample(false);
            example.getIcons().add(
                    new Image(Assets.load("/icon.png").toExternalForm())
            );
            example.setTitle("Nfx Dialog Example");
            example.show();
        });

        openCustomDialogHiddenFromTaskBarBtn.setOnAction(event -> {
            DialogExample example = new DialogExample(true);
            example.getIcons().add(
                    new Image(Assets.load("/icon.png").toExternalForm())
            );
            example.setTitle("Nfx Dialog Hidden from TaskBar Example");
            example.show();
        });

        menuFxWindowBtn.setOnAction(event -> {
            MenuWindow menuWindow = new MenuWindow();
            menuWindow.setTitle("Menu NfxWindow Demo");
            menuWindow.getIcons().add(
                    new Image(Assets.load("/icon.png").toExternalForm())
            );
            menuWindow.show();
        });

        updateDemoBtn.setOnAction(event -> {
            NfxDemoWindow window = new NfxDemoWindow();
            window.show();
        });
    }
}