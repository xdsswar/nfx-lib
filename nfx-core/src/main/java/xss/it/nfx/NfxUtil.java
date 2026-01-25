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
 * Utility class for interacting with native Windows DWM (Desktop Window Manager) features.
 * <p>
 * This class provides the bridge between JavaFX windows and native Windows APIs for customizing
 * window appearance including title bar colors, caption text colors, corner styles, and border
 * colors. It also handles the native library loading and extraction from resources.
 * </p>
 *
 * <h2>Platform Support</h2>
 * <ul>
 *   <li><strong>Windows 10+</strong>: Full functionality via DWM APIs</li>
 *   <li><strong>Windows 7-8.1</strong>: Not supported (methods return early)</li>
 *   <li><strong>macOS/Linux</strong>: Not supported (Windows-specific functionality)</li>
 * </ul>
 *
 * <h2>Native Library Management</h2>
 * <p>
 * The native DLL is automatically extracted from the JAR's resources to the user's home directory
 * ({@code ~/.nfx-libs/}) on first use. The DLL is versioned to ensure compatibility:
 * </p>
 * <pre>
 * ~/.nfx-libs/nfx-core-win64-{version}.dll
 * </pre>
 *
 * <h2>DWM Attribute Mapping</h2>
 * <p>
 * This class uses the following Windows DWM attributes:
 * </p>
 * <table border="1">
 *   <caption>DWM Attributes Used</caption>
 *   <tr><th>Method</th><th>DWM Attribute</th><th>Description</th></tr>
 *   <tr><td>{@link #setTitleBarColor(Color)}</td><td>DWMWA_CAPTION_COLOR</td><td>Title bar background color</td></tr>
 *   <tr><td>{@link #setCaptionColor(Color)}</td><td>DWMWA_TEXT_COLOR</td><td>Title text color</td></tr>
 *   <tr><td>{@link #setCornerPref(CornerPreference)}</td><td>DWMWA_WINDOW_CORNER_PREFERENCE</td><td>Corner rounding style</td></tr>
 *   <tr><td>{@link #setBorderColor(Color)}</td><td>DWMWA_BORDER_COLOR</td><td>Window border color</td></tr>
 * </table>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Get NfxUtil for a JavaFX stage
 * NfxUtil nfxUtil = new NfxUtil(myStage);
 *
 * // Customize window appearance
 * nfxUtil.setTitleBarColor(Color.DARKBLUE);
 * nfxUtil.setCaptionColor(Color.WHITE);
 * nfxUtil.setCornerPref(CornerPreference.ROUND);
 * nfxUtil.setBorderColor(Color.LIGHTGRAY);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * The native library initialization uses double-checked locking with a volatile flag to ensure
 * thread-safe loading. Individual DWM calls are thread-safe as they operate on atomic Windows
 * APIs, but rapid concurrent modifications to the same window may cause visual artifacts.
 * </p>
 *
 * @author XDSSWAR
 * @version 1.0.5
 * @since 1.0.0
 * @see NfxWindow
 * @see CornerPreference
 */
public final class NfxUtil {

    /*
     * =================================================================================================================
     *
     *                                   DWM_WINDOW_CORNER_PREFERENCE Constants
     *
     * These constants map to the Windows DWM_WINDOW_CORNER_PREFERENCE enum values used in
     * DwmSetWindowAttribute. They control window corner rounding on Windows 11+.
     *
     * @see <a href="https://docs.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwm_window_corner_preference">
     *      Microsoft Docs: DWM_WINDOW_CORNER_PREFERENCE</a>
     * =================================================================================================================
     */

    /**
     * Let the system decide on the corner style (DWMWCP_DEFAULT).
     * On Windows 11, this typically results in rounded corners for standard windows.
     */
    private static final int DWM_WCP_DEFAULT = 0;

    /**
     * Do not round window corners (DWMWCP_DONOTROUND).
     * Forces sharp, square corners regardless of system settings.
     */
    private static final int DWM_WCP_DO_NOT_ROUND = 1;

    /**
     * Use standard rounded corners (DWMWCP_ROUND).
     * Applies the default Windows 11 corner radius (typically 8 pixels).
     */
    private static final int DWM_WCP_ROUND = 2;

    /**
     * Use small rounded corners (DWMWCP_ROUNDSMALL).
     * Applies a more subtle rounding with a smaller radius than standard.
     */
    private static final int DWM_WCP_ROUND_SMALL = 3;

    /**
     * The native HWND (window handle) for the associated JavaFX window.
     * <p>
     * This handle is obtained via JNI by extracting the platform window from
     * JavaFX's glass windowing system. It's required for all DWM API calls.
     * A value of 0 indicates the handle could not be obtained (e.g., non-Windows
     * platform or window not yet shown).
     * </p>
     */
    private final long hWnd;

    /**
     * Creates an NfxUtil instance for the specified JavaFX window.
     * <p>
     * The constructor retrieves the native HWND (window handle) from the JavaFX window
     * using JNI. This handle is cached and used for all subsequent DWM API calls.
     * </p>
     * <p>
     * <strong>Important:</strong> The window must be shown (realized) before constructing
     * NfxUtil, otherwise the native handle cannot be obtained. Typically, this is done
     * in response to the {@code WINDOW_SHOWING} event.
     * </p>
     *
     * @param window The JavaFX Window (Stage, or other Window subclass) to customize
     * @throws NullPointerException if window is null and running on Windows 10+
     */
    public NfxUtil(Window window) {
        if (windows10OrLater) {
            this.hWnd = getNativeHandle(window);
        } else {
            hWnd = 0L;
        }
    }

    /**
     * Returns the native HWND (window handle) for this window.
     * <p>
     * The HWND is a Windows-specific identifier that uniquely identifies the window
     * to the operating system. This can be used for direct Win32 API calls if needed.
     * </p>
     *
     * @return the native window handle, or 0 if not available (non-Windows platform
     *         or window not yet shown)
     */
    public long getHWnd() {
        if (!windows10OrLater) return 0L;
        return hWnd;
    }

    /**
     * Sets the title bar background color using the DWM DWMWA_CAPTION_COLOR attribute.
     * <p>
     * This changes the background color of the window's title bar area. On Windows 10,
     * this requires build 1903 or later. On Windows 11, this works with all versions.
     * </p>
     * <p>
     * Note: The alpha channel of the color is ignored; only RGB values are used.
     * </p>
     *
     * @param color the desired title bar color (alpha ignored)
     * @see #setTitleBarColor(String) for hex color input
     */
    public void setTitleBarColor(Color color) {
        if (!windows10OrLater) return;
        setTitleBarColor(hWnd, color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Sets the title bar background color using a hexadecimal color string.
     * <p>
     * Accepts standard web color formats: "#RRGGBB" or "RRGGBB".
     * </p>
     *
     * @param hexColor the hex color string (e.g., "#1a1a2e" or "1a1a2e")
     * @see #setTitleBarColor(Color) for Color input
     */
    public void setTitleBarColor(String hexColor) {
        if (!windows10OrLater) return;
        setTitleBarColor(hexToColor(hexColor));
    }


    /**
     * Sets the title bar caption (text) color using the DWM DWMWA_TEXT_COLOR attribute.
     * <p>
     * This changes the color of the window title text displayed in the title bar.
     * Should be set to a color with good contrast against the title bar background.
     * </p>
     *
     * @param color the desired caption text color
     * @see #setCaptionColor(String) for hex color input
     */
    public void setCaptionColor(Color color) {
        if (!windows10OrLater) return;
        setTextColor(hWnd, color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Sets the caption text color using a hexadecimal color string.
     * <p>
     * Accepts standard web color formats: "#RRGGBB" or "RRGGBB".
     * </p>
     *
     * @param hexColor the hex color string (e.g., "#ffffff" or "ffffff")
     * @see #setCaptionColor(Color) for Color input
     */
    public void setCaptionColor(String hexColor) {
        if (!windows10OrLater) return;
        setCaptionColor(hexToColor(hexColor));
    }

    /**
     * Sets the window corner preference using the DWM DWMWA_WINDOW_CORNER_PREFERENCE attribute.
     * <p>
     * This controls how window corners are rendered on Windows 11. On Windows 10, this
     * attribute may have no effect as rounded corners were introduced in Windows 11.
     * </p>
     *
     * @param cornerPref the desired corner style (null values are ignored)
     * @see CornerPreference for available options
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
     * Sets the window border color using the DWM DWMWA_BORDER_COLOR attribute.
     * <p>
     * This changes the color of the 1-pixel border around the window. On Windows 11,
     * this border is visible even on borderless windows when using rounded corners.
     * </p>
     * <p>
     * <strong>FIX: Issue #27</strong> - Fixed color conversion. Color.getRed() returns
     * a value in the range 0.0-1.0, so direct cast to int would always give 0. Now
     * properly multiplies by 255 to convert to the 0-255 range expected by the native API.
     * </p>
     *
     * @param color the desired border color (null values are ignored)
     */
    public void setBorderColor(Color color) {
        if (color == null || !windows10OrLater) return;
        setBorderColor(hWnd,
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }


    /**
     * Focuses (brings to foreground) a window by its title.
     * <p>
     * This method uses the native Windows FindWindow API to locate a window by its
     * title text, then brings it to the foreground using SetForegroundWindow. If the
     * window is minimized, it will be restored first.
     * </p>
     * <p>
     * <strong>Note:</strong> Window titles must match exactly (case-sensitive).
     * </p>
     *
     * @param title the exact title of the window to focus
     */
    public static void focusWindowByTitle(String title) {
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
     *                                      Helper Methods
     *
     * =================================================================================================================
     */


    /**
     * Converts a hexadecimal color string to a JavaFX Color object.
     * <p>
     * Accepts colors with or without the leading "#" prefix. This is a convenience
     * method that wraps {@link Color#web(String)}.
     * </p>
     *
     * <h4>Supported Formats</h4>
     * <ul>
     *   <li>{@code "#RRGGBB"} - Standard hex with hash</li>
     *   <li>{@code "RRGGBB"} - Hex without hash (auto-prefixed)</li>
     *   <li>{@code "#RGB"} - Shorthand hex (expanded to #RRGGBB)</li>
     * </ul>
     *
     * @param hex the hexadecimal color string
     * @return the corresponding JavaFX Color object
     * @throws IllegalArgumentException if the string cannot be parsed as a color
     */
    public static Color hexToColor(String hex) {
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }
        return Color.web(hex);
    }

    /**
     * Checks if the current platform is Windows 10 or later.
     * <p>
     * This method returns the cached result of the platform check performed during
     * class initialization. It does not re-check the platform on each call.
     * </p>
     *
     * @return {@code true} if running on Windows 10 or later, {@code false} otherwise
     */
    public static boolean isWindows() {
        return windows10OrLater;
    }

    /*
     * =================================================================================================================
     *
     *                               Native Library Extraction and Loading
     *
     * The following section handles extracting the native DLL from the JAR's resources
     * and loading it into the JVM. This is a critical initialization step that must
     * complete before any native methods can be called.
     *
     * IMPORTANT: Modifications to this section require careful testing across different
     * deployment scenarios (JAR, module path, classpath, etc.).
     * =================================================================================================================
     */

    /**
     * Cached result of the Windows 10+ platform check.
     * <p>
     * This flag is set during class initialization and determines whether native
     * functionality is available. If {@code false}, all native methods become no-ops.
     * </p>
     */
    private static final boolean windows10OrLater;

    /**
     * Current version of the NFX library.
     * <p>
     * This version is embedded in the DLL filename to allow multiple versions to
     * coexist in the user's library cache without conflicts.
     * </p>
     */
    private static final String VERSION = "1.0.5";

    /**
     * Filename of the native DLL to extract and load.
     * <p>
     * Format: {@code nfx-core-win64-{version}.dll}
     * </p>
     */
    private static final String libName = String.format("nfx-core-win64-%s.dll", VERSION);

    /**
     * Name of the directory in the user's home folder where native libraries are cached.
     * <p>
     * The leading dot makes this a hidden folder on Unix-like systems, though on Windows
     * it will be visible by default.
     * </p>
     */
    private static final String folderName = ".nfx-libs";

    /**
     * Platform-specific file separator character.
     * <p>
     * On Windows this is "\", on Unix-like systems it is "/".
     * </p>
     */
    private static final String _DIR_SEPARATOR = FileSystems.getDefault().getSeparator();

    /**
     * User's home directory path.
     * <p>
     * Used as the base location for the native library cache. Typically:
     * {@code C:\Users\{username}} on Windows.
     * </p>
     */
    private static final String _USER_DIR = System.getProperty("user.home");

    /**
     * Flag indicating whether the native library has been successfully loaded.
     * <p>
     * <strong>FIX: Issue #13</strong> - Made volatile to ensure visibility across threads.
     * Combined with double-checked locking in {@link #init()}, this ensures the library
     * is loaded exactly once even under concurrent initialization attempts.
     * </p>
     */
    private static volatile boolean initialized = false;


    /**
     * Exports a DLL file from the application's resources to the specified destination path.
     *
     * FIX: Issue #12 - Improved resource handling. Now properly handles the case where
     * getResourceAsStream returns null (resource not found) before creating the output file.
     *
     * FIX: Issue #29 - Now deletes incomplete file on write failure. Previously, a partial
     * file would be left behind, and subsequent attempts would skip (file.exists() = true)
     * but the DLL would be corrupted.
     *
     * @param libName   The name of the DLL file.
     * @param destPath  The destination path where the DLL file will be exported.
     * @throws IOException if an I/O error occurs during the export process or resource not found.
     */
    private static void exportDll(final String libName, String destPath) throws IOException {
        File file = new File(destPath);
        if (file.exists()) {
            return;
        }

        // FIX: Issue #12 - Check if resource exists before creating output file
        String resourcePath = String.format("/lib/%s", libName);
        InputStream inputStream = NfxUtil.class.getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new IOException("Native library resource not found: " + resourcePath);
        }

        // FIX: Issue #29 - Track success and delete incomplete file on failure
        boolean success = false;
        try (inputStream; OutputStream outputStream = new FileOutputStream(destPath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            success = true;
        } finally {
            // FIX: Issue #29 - Delete incomplete file on failure
            if (!success && file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }


    /**
     * Create multiple folders inside a specific directory.
     *
     * FIX: Issue #28 - Now checks mkdirs() return value and throws IOException on failure.
     * Previously, mkdirs() failures were silently ignored, leading to cryptic errors later.
     *
     * @param folder String folder name to create
     * @return String path to the created directory
     * @throws IOException if directory creation fails
     */
    private static String createDirs(String folder) throws IOException {
        File baseDir = new File(_USER_DIR);
        File f = new File(baseDir, folder);
        if (!f.exists()) {
            if (!f.mkdirs()) {
                throw new IOException("Failed to create directory: " + f.getAbsolutePath());
            }
        }
        return f.getAbsolutePath()+_DIR_SEPARATOR;
    }


    /**
     * Initializes the jnilib library.
     * FIX: Issue #13 - Added synchronized block for thread-safe initialization.
     * Uses double-checked locking pattern with volatile flag to prevent
     * multiple threads from loading the DLL simultaneously.
     */
    private static void init() throws IOException {
        // FIX: Issue #13 - Double-checked locking for thread-safe initialization
        if (!initialized) {
            synchronized (NfxUtil.class) {
                if (!initialized) {
                    final String lib = String.format("%s%s", createDirs(folderName), libName);
                    exportDll(libName, lib);
                    System.load(lib);
                    initialized = true;
                }
            }
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
