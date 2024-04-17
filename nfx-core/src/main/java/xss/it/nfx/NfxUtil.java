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

import com.sun.it.nfx.Rect;
import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.*;
import java.nio.file.FileSystems;

/**
 * @author XDSSWAR
 * Created on 04/13/2024
 */
public class NfxUtil {
    /**
     * Represents a constant for hit-testing the client area of a window.
     */
    private static final int HT_CLIENT = 1;

    /**
     * Represents a constant for hit-testing the caption/title bar of a window.
     */
    private static final int HT_CAPTION = 2;

    /**
     * Represents a constant for hit-testing the system menu of a window.
     */
    private static final int HT_SYS_MENU = 3;

    /**
     * Represents a constant for hit-testing the minimize button of a window.
     */
    private static final int HT_MIN_BUTTON = 8;

    /**
     * Represents a constant for hit-testing the maximize button of a window.
     */
    private static final int HT_MAX_BUTTON = 9;

    /**
     * Represents a constant for hit-testing the top edge of a window.
     */
    private static final int HT_TOP = 12;

    /**
     * Represents a constant for hit-testing the close button of a window.
     */
    private static final int HT_CLOSE = 20;

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
     * The current Window
     */
    private final Window window;

    /**
     * Flag to know if its AbstractNfxUndecoratedWindow instance
     */
    private volatile boolean isUndecoratedRef = false;

    /**
     * Initializes an instance of NfxUtil with the native handle of the specified window.
     *
     * @param window The window object for which the native handle is to be retrieved.
     */
    public NfxUtil(Window window){
        this.window = window;
        this.isUndecoratedRef = (this.window instanceof AbstractNfxUndecoratedWindow);
        this.hWnd = getNativeHandle(this.window);
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
     * Sets the color of the title bar of the window using RGB color values.
     *
     * @param color The color object representing the desired title bar color.
     */
    public void setCaptionColor(Color color){
        setTextColor(hWnd, color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Sets the text color of the window using a hexadecimal color string.
     *
     * @param hexColor The hexadecimal color string representing the desired title bar color.
     */
    public void setCaptionColor(String hexColor){
        setCaptionColor(hexToColor(hexColor));
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
     * Hides the window from the taskbar.
     *
     * @param hWnd The handle of the window to hide
     * @param hide True to hide the window from the taskbar, false to show it
     */
    private static native void hideFromTaskBar(long hWnd, boolean hide);

    /**
     * This method will change the Window WinProc in the native side.
     * This is only for window that extends from AbstractNfxUndecoratedWindow, do not use it in another normal window
     *
     * @param hWnd The handle of the window to install
     */
    private static native void install(long hWnd);


    /**
     * Native method to update window decorations for the window with the specified handle.
     *
     * @param hWnd The window handle.
     * @param maximized True if the window is maximized, false otherwise.
     */
    private native void update(long hWnd, boolean maximized);


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




    /*
     * =================================================================================================================
     *
     *                                         JNI callables
     *
     * =================================================================================================================
     */

    /**
     * Handles the non-client hit test for the given point (x, y) and resize border flag. Call from JNI
     *
     * @param x                 The x-coordinate of the point.
     * @param y                 The y-coordinate of the point.
     * @param isOnResizeBorder  A boolean flag indicating whether the point is on a resize border.
     * @return The hit test result code.
     */
    private int jniHitTest(int x, int y, boolean isOnResizeBorder ) {
        invalidateSpots();
        /*
         * Scale down mouse x/y because Swing coordinates/values may be scaled on a HiDPI screen.
         */
        Point2D pt = scaleDown(new Point2D(x,y));
        int sx = (int) pt.getX();
        int sy = (int) pt.getY();

        boolean isOnTitleBar = (isUndecoratedRef && (sy < ((AbstractNfxUndecoratedWindow)window).getTitleBarHeight()));
        if (isUndecoratedRef) {
            for (HitSpot spot : ((AbstractNfxUndecoratedWindow) window).getHitSpots()) {
                if (contains(spot.getRect(), sx, sy)) {
                    if (spot.isSystemMenu()) {//system menu
                        spot.setHovered(true);
                        return HT_SYS_MENU;
                    } else if (spot.isMinimize()) {//minimize
                        spot.setHovered(true);
                        return HT_MIN_BUTTON;
                    } else if (spot.isMaximize()) {//maximize
                        spot.setHovered(true);
                        return HT_MAX_BUTTON;
                    } else if (spot.isClose()) {//close
                        spot.setHovered(true);
                        return HT_CLOSE;
                    } else if (spot.isClient()) {//User controls
                        return HT_CLIENT;
                    } else {//Refresh styles
                        spot.setHovered(false);
                    }
                } else {//Invalidate all
                    invalidateSpots();
                }
            }
        }

        if (isOnTitleBar) {
            return isOnResizeBorder ? HT_TOP : HT_CAPTION;
        }
        return isOnResizeBorder ? HT_TOP : HT_CLIENT;
    }


    /**
     * Checks if the window is in fullscreen mode. Call from JNI
     *
     * @return True if the window is in fullscreen mode, false otherwise.
     */
    private boolean jniIsFullScreen(){
        return (isUndecoratedRef && ((AbstractNfxUndecoratedWindow)window).isFullScreen());
    }

    /**
     * Fires a state change event. Call from JNI.
     * This method triggers the firing of a custom event to indicate a state change.
     */
    private void jniFireStateChanged(){
       // window.fireEvent(new Event(BACKGROUND_CHANGE));
    }


    /**
     * Invalidates the hit spots by calling the invalidateSpots() method.
     * This method is invoked from the native side.
     */
    private void jniInvalidateSpots(){
        invalidateSpots();
    }


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
     * Checks if the given point (x, y) is contained within the specified Rectangle2D object.
     *
     * @param rect The Rectangle2D object to check against.
     * @param x    The x-coordinate of the point.
     * @param y    The y-coordinate of the point.
     * @return True if the point is contained within the rectangle, false otherwise.
     */
    private boolean contains(Rectangle2D rect, int x, int y ) {
        return (rect != null && rect.contains( x, y ) );
    }


    /**
     * Scales down the given Point2D object based on the output scale of the current Screen.
     *
     * @param point The Point2D object to scale down.
     * @return The scaled down Point2D object.
     */
    private Point2D scaleDown(Point2D point) {
        Screen screen= Rect.getCurrentScreen(window);
        double scaleX = screen.getOutputScaleX();
        double scaleY = screen.getOutputScaleY();
        double scaledX = point.getX() / scaleX;
        double scaledY = point.getY() / scaleY;
        return new Point2D(clipRound(scaledX), clipRound(scaledY));
    }

    /**
     * Rounds the given double value and returns it as an integer, clipping it to stay within the range of Integer.MIN_VALUE and Integer.MAX_VALUE.
     *
     * @param value The double value to round.
     * @return The rounded integer value, clipped to the range of Integer.MIN_VALUE and Integer.MAX_VALUE.
     */
    private int clipRound( double value ) {
        value -= 0.5;
        if( value < Integer.MIN_VALUE )
            return Integer.MIN_VALUE;
        if( value > Integer.MAX_VALUE )
            return Integer.MAX_VALUE;
        return (int) Math.ceil( value );
    }


    /**
     * Invalidates the hit spots by setting their hover state to false.
     */
    private void invalidateSpots(){
        if (isUndecoratedRef) {
            ((AbstractNfxUndecoratedWindow)window)
                    .getHitSpots()
                    .forEach(hitSpot -> hitSpot.setHovered(false));
        }
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
