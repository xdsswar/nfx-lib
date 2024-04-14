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

import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.io.*;
import java.nio.file.FileSystems;

/**
 * @author XDSSWAR
 * Created on 04/13/2024
 */
public class NfxUtil {
    /**
     * The native handle of the window.
     */
    private final long hWnd;

    /**
     * Initializes an instance of NfxUtil with the native handle of the specified window.
     *
     * @param window The window object for which the native handle is to be retrieved.
     */
    public NfxUtil(Window window){
        hWnd = getNativeHandle(window);
    }

    /**
     * Gets the native handle of the window.
     *
     * @return The native handle of the window.
     */
    public long getHWnd() {
        return hWnd;
    }


    /**
     * Sets the color of the title bar of the window using RGB color values.
     *
     * @param color The color object representing the desired title bar color.
     */
    public void setTitleBarColor(Color color){
        setTitleBarColor(hWnd, color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Sets the color of the title bar of the window using a hexadecimal color string.
     *
     * @param hexColor The hexadecimal color string representing the desired title bar color.
     */
    public void setTitleBarColor(String hexColor){
        setTitleBarColor(hexToColor(hexColor));
    }

    /**
     * Retrieves the native handle of the specified window object.
     *
     * @param window The window object for which the native handle is to be retrieved.
     * @return The native handle of the window.
     */
    private static native long getNativeHandle(Object window);


    /**
     * Sets the color of the title bar of a window specified by its handle.
     *
     * @param hWnd   The handle of the window whose title bar color is to be set.
     * @param red    The red component of the color
     * @param green  The green component of the color
     * @param blue   The blue component of the color
     */
    private static native void setTitleBarColor(long hWnd, double red, double green, double blue);


    /**
     * Converts a hexadecimal color string to a Color object.
     *
     * @param hex The hexadecimal color string.
     * @return The Color object corresponding to the hexadecimal color string.
     */
    public static Color hexToColor(String hex) {
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }
        return Color.web(hex);
    }




    /*
     * =================================================================================================================
     *
     *                                        Export dll utils
     *
     * =================================================================================================================
     */
    /**
     * Version
     */
    private static final String VERSION = "1.0.0";

    /**
     * Name of the library file.
     */
    private static final String libName= String.format("nfx-core-%s.dll", VERSION);

    /**
     * Name of the folder used for jfx-window-helper.
     */
    private static final String folderName=".nfx-libs";

    /**
     * File separator for the current platform.
     */
    private static final String _DIR_SEPARATOR = FileSystems.getDefault().getSeparator();

    /**
     * User's home directory.
     */
    private static final String _USER_DIR = System.getProperty("user.home");

    /**
     * Flag to determine if initialized
     */
    private static boolean initialized= false;


    /**
     * Exports a DLL file from the application's resources to the specified destination path.
     *
     * @param libName   The name of the DLL file.
     * @param destPath  The destination path where the DLL file will be exported.
     * @throws IOException if an I/O error occurs during the export process.
     */
    public static void exportDll(final String libName, String destPath) throws IOException {
        File file=new File(destPath);
        if (file.exists()) {
            return;
        }
        try (InputStream inputStream = NfxUtil.class.getResourceAsStream(String.format("/lib/%s",libName));
             OutputStream outputStream = new FileOutputStream(destPath)) {
            if (inputStream!=null) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        }
    }


    /**
     * Create multiple folders inside a specific directory
     * @param folder String folder name to create
     * @return String
     */
    public static String createDirs(String folder) {
        File baseDir = new File(_USER_DIR);
        File f = new File(baseDir, folder);
        if (!f.exists()) {
            boolean ignored= f.mkdirs();
        }
        return f.getAbsolutePath()+_DIR_SEPARATOR;
    }


    /**
     * Initializes the jnilib library.
     */
    private static void init() throws IOException {
        if (!initialized) {
            final String lib = String.format("%s%s", createDirs(folderName), libName);
            exportDll(libName, lib);
            System.load(lib);
            initialized = true;
        }
    }

    /*
     * Initialize and load the Jni
     */
    static {
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
