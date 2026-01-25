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
 * Enumeration representing the possible states of an NFX window.
 * <p>
 * This enum provides a unified way to track and control window states across the NFX library.
 * It integrates with the native Windows window management system to provide accurate state
 * tracking and synchronization between JavaFX and the underlying Win32 window.
 * </p>
 *
 * <h2>State Transitions</h2>
 * <p>
 * Windows can transition between states based on user actions or programmatic calls:
 * </p>
 * <pre>
 *                    ┌─────────────┐
 *              ┌────▶│   NORMAL    │◀────┐
 *              │     └─────────────┘     │
 *              │            │            │
 *         restore      maximize     minimize
 *              │            │            │
 *              ▼            ▼            ▼
 *     ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
 *     │  MINIMIZED  │ │  MAXIMIZED  │ │ FULL_SCREEN │
 *     └─────────────┘ └─────────────┘ └─────────────┘
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * NfxStage stage = new NfxStage();
 *
 * // Listen for state changes
 * stage.windowStateProperty().addListener((obs, oldState, newState) -> {
 *     switch (newState) {
 *         case MAXIMIZED -> System.out.println("Window maximized");
 *         case MINIMIZED -> System.out.println("Window minimized");
 *         case FULL_SCREEN -> System.out.println("Window is fullscreen");
 *         case NORMAL -> System.out.println("Window restored to normal");
 *     }
 * });
 *
 * // Programmatically change state
 * stage.setWindowState(WindowState.MAXIMIZED);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * State changes should be performed on the JavaFX Application Thread. The library handles
 * synchronization between native Windows messages and JavaFX property updates internally.
 * </p>
 *
 * @author XDSSWAR
 * @version 1.0.5
 * @since 1.0.0
 * @see AbstractNfxUndecoratedWindow#windowStateProperty()
 * @see AbstractNfxUndecoratedWindow#setWindowState(WindowState)
 */
public enum WindowState {

    /**
     * The window is in its normal, restored state.
     * <p>
     * In this state, the window:
     * <ul>
     *   <li>Is not maximized, minimized, or in fullscreen mode</li>
     *   <li>Can be freely resized and moved by the user</li>
     *   <li>Occupies its user-defined or default size and position</li>
     *   <li>Shows standard window chrome (if not undecorated)</li>
     * </ul>
     * <p>
     * This is typically the initial state when a window is first shown, unless
     * specifically configured otherwise.
     */
    NORMAL,

    /**
     * The window is minimized to the taskbar.
     * <p>
     * In this state, the window:
     * <ul>
     *   <li>Is not visible on screen (unless hovering over taskbar preview)</li>
     *   <li>Appears as a button/icon in the Windows taskbar</li>
     *   <li>Retains its previous state (NORMAL, MAXIMIZED) for restoration</li>
     *   <li>Does not receive mouse or keyboard input directly</li>
     * </ul>
     * <p>
     * When restored from MINIMIZED, the window returns to its previous state
     * (either NORMAL or MAXIMIZED), tracked internally via {@code prevState}.
     *
     * @see AbstractNfxUndecoratedWindow#prevState
     */
    MINIMIZED,

    /**
     * The window is maximized to fill the screen (excluding taskbar).
     * <p>
     * In this state, the window:
     * <ul>
     *   <li>Fills the entire work area of the current monitor</li>
     *   <li>Respects the taskbar and other app bars (unless auto-hidden)</li>
     *   <li>Cannot be resized (resize borders are disabled)</li>
     *   <li>Cannot be moved by dragging the title bar</li>
     *   <li>May have adjusted corner rendering (no rounded corners)</li>
     * </ul>
     * <p>
     * The native side sends {@code WM_NCCALCSIZE} to adjust the client area
     * appropriately when entering/exiting this state.
     */
    MAXIMIZED,

    /**
     * The window is in exclusive fullscreen mode.
     * <p>
     * In this state, the window:
     * <ul>
     *   <li>Covers the entire screen including the taskbar</li>
     *   <li>Has no visible window chrome or borders</li>
     *   <li>Receives priority for rendering and input</li>
     *   <li>May disable DWM composition for better performance</li>
     *   <li>Is ideal for games, video players, and presentations</li>
     * </ul>
     * <p>
     * Unlike MAXIMIZED, FULL_SCREEN completely hides system UI elements and
     * provides the application with full control over the display.
     */
    FULL_SCREEN
}
