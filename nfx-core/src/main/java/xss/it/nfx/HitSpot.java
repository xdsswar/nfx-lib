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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.Region;

/**
 * Represents a hit-testable region within an NFX window for native Windows integration.
 * <p>
 * A {@code HitSpot} defines an interactive area in the window's non-client region that can
 * respond to Windows hit-testing messages (WM_NCHITTEST). This enables custom window chrome
 * components (like title bar buttons) to interact properly with the native Windows window
 * management system, including Snap Layouts, window dragging, and system menu integration.
 * </p>
 *
 * <h2>Purpose and Architecture</h2>
 * <p>
 * In undecorated JavaFX windows, the native Windows shell loses knowledge of where UI elements
 * like close, minimize, and maximize buttons are located. HitSpot bridges this gap by:
 * </p>
 * <ul>
 *   <li>Mapping JavaFX Region coordinates to native screen coordinates</li>
 *   <li>Providing hit-test type information (close, maximize, minimize, system menu, client)</li>
 *   <li>Tracking hover state for visual feedback synchronization</li>
 *   <li>Supporting thread-safe coordinate queries from native callbacks</li>
 * </ul>
 *
 * <h2>Hit-Test Types</h2>
 * <p>
 * Each HitSpot can represent one of the following interaction types:
 * </p>
 * <table border="1">
 *   <caption>HitSpot Types and Their Windows Behavior</caption>
 *   <tr><th>Type</th><th>Win32 HTCODE</th><th>Behavior</th></tr>
 *   <tr><td>Close</td><td>HTCLOSE</td><td>Triggers window close, Snap Layout close button</td></tr>
 *   <tr><td>Maximize</td><td>HTMAXBUTTON</td><td>Toggles maximized state, shows Snap Layouts on hover</td></tr>
 *   <tr><td>Minimize</td><td>HTMINBUTTON</td><td>Minimizes to taskbar</td></tr>
 *   <tr><td>System Menu</td><td>HTSYSMENU</td><td>Shows system menu on right-click/double-click</td></tr>
 *   <tr><td>Client</td><td>HTCLIENT</td><td>Normal client area (default if no flags set)</td></tr>
 * </table>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a HitSpot for a maximize button
 * Button maxButton = new Button();
 * HitSpot maxSpot = HitSpot.builder()
 *     .window(nfxStage)
 *     .control(maxButton)
 *     .maximize(true)
 *     .build();
 *
 * // Register with the window
 * nfxStage.registerHitSpot(maxSpot);
 *
 * // Listen for hover state changes (for visual feedback)
 * maxSpot.hoveredProperty().addListener((obs, wasHovered, isHovered) -> {
 *     if (isHovered) {
 *         maxButton.setStyle("-fx-background-color: lightblue;");
 *     } else {
 *         maxButton.setStyle("");
 *     }
 * });
 * }</pre>
 *
 * <h2>Windows 11 Snap Layouts Integration</h2>
 * <p>
 * When the user hovers over a maximize button HitSpot on Windows 11, the system displays
 * the Snap Layouts flyout menu. This requires accurate hit-testing coordinates to be
 * returned to the native layer via {@link #getRect()} or {@link #getRectSafe()}.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * The {@link #getRectSafe()} method is designed for thread-safe access from JNI callbacks
 * that may execute on the Windows message pump thread. It returns {@code null} instead of
 * throwing exceptions if coordinates cannot be computed safely.
 * </p>
 *
 * <h2>Builder Pattern</h2>
 * <p>
 * HitSpot uses the Builder pattern for construction, ensuring all required fields are set
 * and the object is properly initialized. The window parameter is mandatory.
 * </p>
 *
 * @author XDSSWAR
 * @version 1.0.5
 * @since 1.0.0
 * @see NfxStage#addClientAreas(Region...)
 * @see NfxStage#getHitSpots()
 */
public final class HitSpot {

    /**
     * The JavaFX Region that defines the visual and spatial bounds of this HitSpot.
     * <p>
     * This control's bounds (in scene coordinates) are used to determine whether a
     * native Windows mouse event falls within this hit-testable area. The control
     * must be part of the scene graph for coordinate transformations to work correctly.
     * </p>
     */
    private final Region control;

    /**
     * Indicates whether this HitSpot represents a close button.
     * <p>
     * When {@code true}, the native layer returns {@code HTCLOSE} for hit-tests within
     * this region, enabling standard Windows close button behavior including keyboard
     * shortcuts (Alt+F4) and window closure on click.
     * </p>
     */
    private final boolean close;

    /**
     * Indicates whether this HitSpot represents a maximize/restore button.
     * <p>
     * When {@code true}, the native layer returns {@code HTMAXBUTTON} for hit-tests
     * within this region. On Windows 11, this enables the Snap Layouts flyout menu
     * when the user hovers over the button, providing window arrangement options.
     * </p>
     */
    private final boolean maximize;

    /**
     * Indicates whether this HitSpot represents a minimize button.
     * <p>
     * When {@code true}, the native layer returns {@code HTMINBUTTON} for hit-tests
     * within this region, enabling standard Windows minimize behavior that sends
     * the window to the taskbar.
     * </p>
     */
    private final boolean minimize;

    /**
     * Indicates whether this HitSpot represents a system menu icon/area.
     * <p>
     * When {@code true}, the native layer returns {@code HTSYSMENU} for hit-tests
     * within this region. This enables the system menu to appear on right-click
     * or double-click, providing options like Move, Size, Minimize, Maximize, and Close.
     * </p>
     */
    private final boolean systemMenu;

    /**
     * Indicates whether this HitSpot represents a client (non-special) area.
     * <p>
     * This is automatically set to {@code true} when none of the special types
     * (close, maximize, minimize, systemMenu) are enabled. Client areas receive
     * {@code HTCLIENT} in hit-testing, allowing normal mouse interaction without
     * special window management behavior.
     * </p>
     */
    private final boolean client;

    /*
     * Observable property tracking whether the native hover state is active.
     * This property is updated by the native layer when Windows sends hover
     * messages (WM_NCMOUSEMOVE, WM_NCMOUSELEAVE) over the HitSpot region.
     * Lazy-initialized on first access.
     */
    private BooleanProperty hovered;

    /**
     * Reference to the parent window containing this HitSpot.
     * <p>
     * Required for coordinate transformation (to determine if window is maximized)
     * and for lifecycle management. This reference is mandatory and must be set
     * via the Builder.
     * </p>
     */
    private AbstractNfxUndecoratedWindow window = null;

    /**
     * Constructs a HitSpot object using the Builder pattern.
     * <p>
     * This private constructor is called by {@link Builder#build()} after all
     * configuration has been set. It validates required parameters and computes
     * derived values like the {@code client} flag.
     * </p>
     *
     * @param builder The builder containing all configuration options
     * @throws NullPointerException if {@code builder.window} is null
     */
    private HitSpot(Builder builder) {
        if (builder.window == null) {
            throw new NullPointerException("Window can't be null");
        }
        this.window = builder.window;
        this.control = builder.control;
        this.close = builder.close;
        this.maximize = builder.maximize;
        this.minimize = builder.minimize;
        this.systemMenu = builder.systemMenu;
        // Client is true when no special type is set
        this.client = !close && !maximize && !minimize && !systemMenu;
    }


    /**
     * Returns the observable property tracking the native hover state.
     * <p>
     * This property is updated by the native layer when Windows sends mouse tracking
     * messages for this HitSpot region. JavaFX controls can bind to this property
     * to synchronize their visual state with the native hover detection.
     * </p>
     * <p>
     * The property is lazy-initialized on first access. If hover tracking is not
     * needed, avoid calling this method to reduce memory overhead.
     * </p>
     *
     * <h4>Example: Synchronized Hover Styling</h4>
     * <pre>{@code
     * hitSpot.hoveredProperty().addListener((obs, was, is) -> {
     *     button.pseudoClassStateChanged(HOVER_CLASS, is);
     * });
     * }</pre>
     *
     * @return the observable BooleanProperty for hover status (never null)
     */
    public BooleanProperty hoveredProperty() {
        if (hovered == null) {
            hovered = new SimpleBooleanProperty(this, "hovered", false);
        }
        return hovered;
    }

    /**
     * Checks if this HitSpot is currently being hovered by the native mouse cursor.
     * <p>
     * This reflects the native Windows hover state, which may differ slightly from
     * JavaFX's own hover detection due to timing and coordinate system differences.
     * </p>
     *
     * @return {@code true} if the native cursor is hovering over this HitSpot,
     *         {@code false} otherwise
     */
    public boolean isHovered() {
        return hoveredProperty().get();
    }

    /**
     * Sets the hover status of this HitSpot.
     * <p>
     * This method is typically called by the native layer when Windows sends
     * mouse tracking messages. It can also be called programmatically to force
     * a particular hover state, though this may be overwritten by subsequent
     * native events.
     * </p>
     *
     * @param hovered {@code true} to indicate hover state is active,
     *                {@code false} to indicate hover has ended
     */
    public void setHovered(boolean hovered) {
        this.hoveredProperty().set(hovered);
    }


    /**
     * Gets the region associated with the HitSpot.
     *
     * @return The control region.
     */
    public Region getControl() {
        return control;
    }

    /**
     * Checks if the close interaction is enabled.
     *
     * @return True if close interaction is enabled, false otherwise.
     */
    public boolean isClose() {
        return close;
    }

    /**
     * Checks if the maximize interaction is enabled.
     *
     * @return True if maximize interaction is enabled, false otherwise.
     */
    public boolean isMaximize() {
        return maximize;
    }

    /**
     * Checks if the minimize interaction is enabled.
     *
     * @return True if minimize interaction is enabled, false otherwise.
     */
    public boolean isMinimize() {
        return minimize;
    }

    /**
     * Checks if the system menu interaction is enabled.
     *
     * @return True if system menu interaction is enabled, false otherwise.
     */
    public boolean isSystemMenu() {
        return systemMenu;
    }

    /**
     * Checks if the client interaction is enabled.
     *
     * @return True if client interaction is enabled, false otherwise.
     */
    public boolean isClient() {
        return client;
    }


    /**
     * Returns the Rectangle2D representing the bounds of the HitSpot in the scene coordinates.
     * FIX: Issue #4 &amp; #9 - Added null checks and defensive programming.
     *
     * @return The Rectangle2D representing the bounds of the HitSpot.
     * @throws NullPointerException if control or window is null (use getRectSafe() for null-safe access)
     */
    public Rectangle2D getRect(){
        // FIX: Issue #9 - Add null checks
        if (control == null) {
            throw new NullPointerException("Control is null");
        }
        if (window == null) {
            throw new NullPointerException("Window is null");
        }
        return createFromBounds(control.localToScene(
                control.getBoundsInLocal()),
                window.getWindowState()== WindowState.MAXIMIZED
        );
    }

    /**
     * Returns the Rectangle2D representing the bounds of the HitSpot in the scene coordinates.
     * This is a thread-safe version that returns null instead of throwing exceptions.
     *
     * FIX: Issue #4 - Thread-safe method for use from JNI callbacks.
     * This method catches exceptions and returns null, making it safe to call from non-FX threads.
     *
     * @return The Rectangle2D representing the bounds of the HitSpot, or null if unavailable.
     */
    public Rectangle2D getRectSafe() {
        try {
            // FIX: Issue #4 & #9 - Defensive null checks
            if (control == null || window == null) {
                return null;
            }

            // Get bounds - may return null or contain NaN if control not laid out
            var boundsInLocal = control.getBoundsInLocal();
            if (boundsInLocal == null || Double.isNaN(boundsInLocal.getWidth())) {
                return null;
            }

            var boundsInScene = control.localToScene(boundsInLocal);
            if (boundsInScene == null) {
                return null;
            }

            WindowState state = window.getWindowState();
            return createFromBounds(boundsInScene, state == WindowState.MAXIMIZED);

        } catch (Exception e) {
            // FIX: Issue #4 - Return null on any error for thread-safety
            return null;
        }
    }

    /**
     * Creates a {@link Rectangle2D} from the given {@link Bounds} object.
     * <p>
     * This utility method converts JavaFX Bounds (which have minX, minY, maxX, maxY, width, height)
     * to a Rectangle2D (which has x, y, width, height). The conversion preserves the coordinate
     * space of the supplied bounds without any transformations.
     * </p>
     *
     * @param bounds the bounds to convert (must not be null)
     * @param max    hint indicating whether the window is maximized; reserved for future use
     *               where maximized windows may need coordinate adjustments
     * @return a new Rectangle2D with the same position and dimensions as the input bounds
     * @throws NullPointerException if bounds is null
     */
    public static Rectangle2D createFromBounds(Bounds bounds, boolean max) {
        return new Rectangle2D(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
    }

    /**
     * Builder class for constructing {@link HitSpot} instances with a fluent API.
     * <p>
     * The Builder pattern ensures that HitSpot objects are properly initialized with
     * all required dependencies before use. The {@code window} parameter is mandatory
     * and will cause construction to fail if not provided.
     * </p>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * HitSpot closeButton = HitSpot.builder()
     *     .window(myNfxStage)
     *     .control(closeButtonRegion)
     *     .close(true)
     *     .build();
     * }</pre>
     *
     * <h2>Type Exclusivity</h2>
     * <p>
     * While multiple type flags (close, maximize, minimize, systemMenu) can technically
     * be set, it's recommended to set only one per HitSpot for predictable behavior.
     * If none are set, the HitSpot defaults to client area behavior.
     * </p>
     *
     * @see HitSpot#builder()
     */
    public final static class Builder {

        /**
         * The JavaFX Region that defines the bounds of the HitSpot.
         */
        private Region control;

        /**
         * Whether this HitSpot acts as a close button (HTCLOSE).
         * Default: {@code false}
         */
        private boolean close = false;

        /**
         * Whether this HitSpot acts as a maximize button (HTMAXBUTTON).
         * Default: {@code false}
         */
        private boolean maximize = false;

        /**
         * Whether this HitSpot acts as a minimize button (HTMINBUTTON).
         * Default: {@code false}
         */
        private boolean minimize = false;

        /**
         * Whether this HitSpot acts as a system menu area (HTSYSMENU).
         * Default: {@code false}
         */
        private boolean systemMenu = false;

        /**
         * The parent window containing this HitSpot. Required parameter.
         */
        private AbstractNfxUndecoratedWindow window;

        /**
         * Sets the control for the HitSpot.
         *
         * @param control the Node control to be set
         * @return the Builder object
         */
        public Builder control(Region control) {
            this.control = control;
            return this;
        }


        /**
         * Sets whether the HitSpot should have a close functionality.
         *
         * @param close true if the HitSpot should have a close functionality, false otherwise
         * @return the Builder object
         */
        public Builder close(boolean close) {
            this.close = close;
            return this;
        }

        /**
         * Sets whether the HitSpot should have a maximize functionality.
         *
         * @param maximize true if the HitSpot should have a maximize functionality, false otherwise
         * @return the Builder object
         */
        public Builder maximize(boolean maximize) {
            this.maximize = maximize;
            return this;
        }

        /**
         * Sets whether the HitSpot should have a minimize functionality.
         *
         * @param minimize true if the HitSpot should have a minimize functionality, false otherwise
         * @return the Builder object
         */
        public Builder minimize(boolean minimize) {
            this.minimize = minimize;
            return this;
        }

        /**
         * Sets whether the HitSpot should have a system menu functionality.
         *
         * @param systemMenu true if the HitSpot should have a system menu functionality, false otherwise
         * @return the Builder object
         */
        public Builder systemMenu(boolean systemMenu) {
            this.systemMenu = systemMenu;
            return this;
        }

        /**
         * Sets the window instance for the builder and returns the builder for chaining.
         * <p>
         * This method assigns the specified {@link AbstractNfxUndecoratedWindow} instance to the builder,
         * allowing customization of the window's properties and behaviors. By returning the builder itself,
         * this method supports method chaining, enabling a fluent and flexible way to configure the window.
         *
         * @param window the {@link AbstractNfxUndecoratedWindow} instance to be used in the builder
         * @return the current {@code Builder} instance for chaining further configuration methods
         */
        public Builder window(AbstractNfxUndecoratedWindow window) {
            this.window = window;
            return this;
        }

        /**
         * Builds and returns the HitSpot object using the Builder's configuration.
         *
         * @return the constructed HitSpot object
         */
        public HitSpot build() {
            return new HitSpot(this);
        }

    }


    /**
     * Creates a new Builder instance for constructing a HitSpot.
     * <p>
     * This is the entry point for the HitSpot fluent builder API. At minimum,
     * the {@link Builder#window(AbstractNfxUndecoratedWindow)} method must be
     * called before {@link Builder#build()}.
     * </p>
     *
     * <h4>Example Usage</h4>
     * <pre>{@code
     * HitSpot spot = HitSpot.builder()
     *     .window(nfxStage)
     *     .control(myButton)
     *     .maximize(true)
     *     .build();
     * }</pre>
     *
     * @return a new Builder instance ready for configuration
     */
    public static Builder builder() {
        return new Builder();
    }

}
