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
public final class NfxUtil {
    /**
     * Represents a default window composition parameter.
     */
    private static final int DWM_WCP_DEFAULT = 0;

    /**
     * Represents a window composition parameter to avoid rounding.
     */
    private static final int DWM_WCP_DO_NOT_ROUND = 1;

    /**
     * Represents a window composition parameter to round the window.
     */
    private static final int DWM_WCP_ROUND = 2;

    /**
     * Represents a window composition parameter to round the window with a small radius.
     */
    private static final int DWM_WCP_ROUND_SMALL = 3;

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
       if (windows10OrLater) {
           this.hWnd = getNativeHandle(window);
       }
       else {
           hWnd = 0L;
       }
    }

    /**
     * Gets the native handle of the window.
     *
     * @return The native handle of the window.
     */
    public long getHWnd() {
        if (!windows10OrLater) return 0L;
        return hWnd;
    }

    /**
     * Sets the color of the title bar of the window using RGB color values.
     *
     * @param color The color object representing the desired title bar color.
     */
    public void setTitleBarColor(Color color){
        if (!windows10OrLater) return;
        setTitleBarColor(hWnd, color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Sets the color of the title bar of the window using a hexadecimal color string.
     *
     * @param hexColor The hexadecimal color string representing the desired title bar color.
     */
    public void setTitleBarColor(String hexColor){
        if (!windows10OrLater) return;
        setTitleBarColor(hexToColor(hexColor));
    }


    /**
     * Sets the color of the title bar of the window using RGB color values.
     *
     * @param color The color object representing the desired title bar color.
     */
    public void setCaptionColor(Color color){
        if (!windows10OrLater) return;
        setTextColor(hWnd, color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Sets the text color of the window using a hexadecimal color string.
     *
     * @param hexColor The hexadecimal color string representing the desired title bar color.
     */
    public void setCaptionColor(String hexColor){
        if (!windows10OrLater) return;
        setCaptionColor(hexToColor(hexColor));
    }

    /**
     * Sets the corner preference of the window.
     *
     * @param cornerPref The corner preference to set
     */
    public void setCornerPref(CornerPreference cornerPref) {
        if (cornerPref == null || !windows10OrLater) return;
        switch (cornerPref) {
            case NOT_ROUND -> setCornerPreference(hWnd, DWM_WCP_DO_NOT_ROUND);
            case ROUND -> setCornerPreference(hWnd, DWM_WCP_ROUND);
            case ROUND_SMALL -> setCornerPreference(hWnd, DWM_WCP_ROUND_SMALL);
            case DEFAULT -> setCornerPreference(hWnd, DWM_WCP_DEFAULT);
        }
    }

    /**
     * Sets the border color of the window.
     *
     * @param color The color to set as the border color
     */
    public void setBorderColor(Color color) {
        if (color == null || !windows10OrLater) return;
        setBorderColor(hWnd, (int) color.getRed(), (int) color.getGreen(), (int) color.getBlue());
    }


    /**
     * Focuses the window with the specified title using a native method.
     *
     * @param title The title of the window to focus.
     */
    public static void focusWindowByTitle(String title){
        if (windows10OrLater) {
            focusWindow(title);
        }
    }



    /*
     * =================================================================================================================
     *
     *                                        NATIVE METHODS
     *
     * =================================================================================================================
     */


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
     * Sets the caption color of a window specified by its handle.
     *
     * @param hWnd   The handle of the window whose title bar color is to be set.
     * @param red    The red component of the color
     * @param green  The green component of the color
     * @param blue   The blue component of the color
     */
    private static native void setTextColor(long hWnd, double red, double green, double blue);

    /**
     * Native method to set the corner preference for the window with the specified handle.
     *
     * @param hWnd The window handle.
     * @param pref The corner preference.
     * @return True if the corner preference is set successfully, false otherwise.
     */
    private native boolean setCornerPreference(long hWnd, int pref);

    /**
     * Native method to set the border color for the window with the specified handle.
     *
     * @param hWnd The window handle.
     * @param red   The red component of the border color.
     * @param green The green component of the border color.
     * @param blue  The blue component of the border color.
     */
    private native boolean setBorderColor(long hWnd, int red, int green, int blue);

    /**
     * Focuses the window with the specified name using a native method.
     *
     * @param name The name of the window to focus.
     */
    private static native void focusWindow(String name);

    /*
     * =================================================================================================================
     *
     *                                         Helpers
     *
     * =================================================================================================================
     */


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

    /**
     * Check if we are on windows
     * @return tue if yes
     */
    public static boolean isWindows(){
        return windows10OrLater;
    }

    /*
     * =================================================================================================================
     *
     *                                        Export dll utils
     *                                         DO NOT TOUCH
     *
     * =================================================================================================================
     */
    /**
     * Windows flag
     */
    private static final boolean windows10OrLater;

    /**
     * Version
     */
    private static final String VERSION = "1.0.3";

    /**
     * Name of the library file.
     */
    private static final String libName= String.format("nfx-core-win64-%s.dll", VERSION);

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
    private static void exportDll(final String libName, String destPath) throws IOException {
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
    private static String createDirs(String folder) {
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

    /**
     * Returns {@code true} only on Windows 10 or later.
     * <p>
     * Checks {@code os.name} starts with "Windows" (case-insensitive) and parses
     * {@code os.version}'s leading numeric portion (e.g., "10.0", "6.3"). Returns
     * {@code true} iff the parsed version is {@code >= 10.0}.
     * <br>
     * Note: Many JDKs report Windows 11 as {@code os.name="Windows 10"} with
     * {@code os.version="10.0"}, which still satisfies the {@code >= 10.0} check.
     *
     * @return {@code true} if running on Windows and {@code os.version >= 10.0}, else {@code false}
     */
    public static boolean isWindows10OrLater() {
        String name = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        if (!name.startsWith("windows")) return false;

        String ver = System.getProperty("os.version", "");
        double v = parseLeadingVersion(ver);
        return v >= 10.0;
    }

    /**
     * Parses the leading numeric portion of a version string (e.g., "10.0", "6.3").
     * @param s version
     * @return double
     */
    private static double parseLeadingVersion(String s) {
        if (s == null || s.isEmpty()) return -1;
        int i = 0;
        boolean seenDot = false;
        StringBuilder sb = new StringBuilder(8);
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            } else if (c == '.' && !seenDot) {
                seenDot = true;
                sb.append(c);
            } else {
                break; // stop at first non [0-9 or one dot]
            }
        }
        try {
            return Double.parseDouble(sb.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /*
     * Initialize and load the Jni
     */
    static {
        windows10OrLater = isWindows10OrLater();
        if (windows10OrLater) {
            try {
                init();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
