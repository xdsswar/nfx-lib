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

/**
 * Enumeration representing window corner preferences for Windows 11+ DWM (Desktop Window Manager).
 * <p>
 * This enum maps directly to the Windows DWM_WINDOW_CORNER_PREFERENCE values used in the
 * DwmSetWindowAttribute API. It allows JavaFX applications to control the corner rounding
 * style of their windows on Windows 11 and later.
 * </p>
 *
 * <h2>Platform Support</h2>
 * <ul>
 *   <li>Windows 11+: Full support for all corner preferences</li>
 *   <li>Windows 10: Limited or no effect (system defaults apply)</li>
 *   <li>Other platforms: No effect (Windows-specific feature)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * NfxWindow window = new NfxWindow();
 * window.setCornerPreference(CornerPreference.ROUND);
 * window.show();
 * }</pre>
 *
 * <h2>DWM Mapping</h2>
 * <table border="1">
 *   <tr><th>Java Enum</th><th>DWM Constant</th><th>Value</th></tr>
 *   <tr><td>DEFAULT</td><td>DWMWCP_DEFAULT</td><td>0</td></tr>
 *   <tr><td>NOT_ROUND</td><td>DWMWCP_DONOTROUND</td><td>1</td></tr>
 *   <tr><td>ROUND</td><td>DWMWCP_ROUND</td><td>2</td></tr>
 *   <tr><td>ROUND_SMALL</td><td>DWMWCP_ROUNDSMALL</td><td>3</td></tr>
 * </table>
 *
 * @author XDSSWAR
 * @version 1.0.5
 * @since 1.0.0
 * @see NfxWindow#setCornerPreference(CornerPreference)
 * @see <a href="https://docs.microsoft.com/en-us/windows/win32/api/dwmapi/ne-dwmapi-dwm_window_corner_preference">
 *      DWM_WINDOW_CORNER_PREFERENCE (Microsoft Docs)</a>
 */
public enum CornerPreference {

    /**
     * The system default corner style.
     * <p>
     * Allows the operating system to determine the appropriate corner rounding
     * based on the window style and system settings. On Windows 11, this typically
     * results in rounded corners for standard windows.
     * </p>
     * <p>
     * Maps to: {@code DWMWCP_DEFAULT (0)}
     * </p>
     */
    DEFAULT,

    /**
     * Square corners with no rounding.
     * <p>
     * Forces the window to have sharp, square corners regardless of the
     * system's default styling. Useful for applications that require
     * pixel-perfect edges or a more traditional appearance.
     * </p>
     * <p>
     * Maps to: {@code DWMWCP_DONOTROUND (1)}
     * </p>
     */
    NOT_ROUND,

    /**
     * Fully rounded corners.
     * <p>
     * Applies the standard Windows 11 rounded corner style with the default
     * corner radius (typically 8 pixels). This is the most common style for
     * modern Windows applications.
     * </p>
     * <p>
     * Maps to: {@code DWMWCP_ROUND (2)}
     * </p>
     */
    ROUND,

    /**
     * Small rounded corners.
     * <p>
     * Applies a subtle rounding to the window corners with a smaller radius
     * than {@link #ROUND}. Useful for secondary windows, dialogs, or when
     * a less pronounced rounding effect is desired.
     * </p>
     * <p>
     * Maps to: {@code DWMWCP_ROUNDSMALL (3)}
     * </p>
     */
    ROUND_SMALL
}
