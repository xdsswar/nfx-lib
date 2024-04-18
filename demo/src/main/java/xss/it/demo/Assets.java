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

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author XDSSWAR
 * Created on 04/17/2024
 */
public class Assets {

    /**
     * This method loads a URL for a given location.
     *
     * @param location The location of the resource to load.
     * @return A URL object representing the resource's location.
     */
    public static URL load(final String location) {
        return Assets.class.getResource(location);
    }

    /**
     * Retrieves an InputStream for a given resource location using the class loader.
     *
     * @param location The resource location.
     * @return An InputStream for the specified resource.
     */
    public static InputStream stream(final String location) {
        return Assets.class.getResourceAsStream(location);
    }


    /**
     * Loads an FXML file from the specified location and sets the controller.
     *
     * @param location   The location of the FXML file to load.
     * @param controller The controller object to be set for the loaded FXML file.
     * @return           The root node of the loaded FXML file as a Parent object.
     * @throws IOException If an I/O error occurs during loading.
     */
    public static Parent load(String location, Object controller) throws IOException {
        FXMLLoader loader = new FXMLLoader(load(location));
        loader.setController(controller);
        return loader.load();
    }

}
