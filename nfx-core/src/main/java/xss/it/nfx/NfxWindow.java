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

import javafx.animation.PauseTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/**
 * A JavaFX Stage with native Windows DWM customization support.
 * <p>
 * NfxWindow extends the standard JavaFX {@link Stage} to provide seamless integration with
 * Windows Desktop Window Manager (DWM) features. It allows customization of window chrome
 * elements that are normally controlled by the operating system, including title bar colors,
 * caption text colors, corner styles, and border colors.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><strong>Title Bar Color:</strong> Custom background color for the title bar</li>
 *   <li><strong>Caption Color:</strong> Custom text color for the window title</li>
 *   <li><strong>Corner Preference:</strong> Control window corner rounding (Windows 11+)</li>
 *   <li><strong>Border Color:</strong> Custom window border color</li>
 * </ul>
 *
 * <h2>Platform Support</h2>
 * <ul>
 *   <li><strong>Windows 10+</strong>: Full DWM customization support</li>
 *   <li><strong>Other platforms</strong>: Falls back to standard Stage behavior</li>
 * </ul>
 *
 * <h2>Initialization Lifecycle</h2>
 * <p>
 * DWM customization requires a native window handle (HWND), which is only available after
 * the window is shown. NfxWindow handles this automatically:
 * </p>
 * <ol>
 *   <li>Window construction sets up event handlers</li>
 *   <li>{@code WINDOW_SHOWING} event triggers native initialization</li>
 *   <li>A brief delay (3ms) ensures the native window is fully created</li>
 *   <li>DWM attributes are applied and property listeners are registered</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * NfxWindow window = new NfxWindow();
 *
 * // Configure DWM properties
 * window.setTitleBarColor(Color.web("#1a1a2e"));
 * window.setCaptionColor(Color.WHITE);
 * window.setCornerPreference(CornerPreference.ROUND);
 * window.setWindowBorder(Color.LIGHTGRAY);
 *
 * // Show the window
 * window.setScene(new Scene(root, 800, 600));
 * window.show();
 * }</pre>
 *
 * <h2>Property Binding</h2>
 * <p>
 * All DWM properties are observable and support binding. Changes to properties are
 * automatically propagated to the native window:
 * </p>
 * <pre>{@code
 * // Dynamic theme switching
 * BooleanProperty darkMode = new SimpleBooleanProperty(false);
 * window.titleBarColorProperty().bind(
 *     Bindings.when(darkMode)
 *         .then(Color.web("#1a1a2e"))
 *         .otherwise(Color.WHITE)
 * );
 * }</pre>
 *
 * <h2>Subclassing</h2>
 * <p>
 * NfxWindow is designed to be subclassed. {@link NfxStage} extends this class to add
 * support for undecorated windows with custom chrome. Protected methods like
 * {@link #invalidateSpots()} and {@link #update(boolean, boolean)} can be overridden
 * to respond to state changes.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Like all JavaFX nodes, NfxWindow must be accessed from the JavaFX Application Thread.
 * Internal synchronization protects against duplicate listener registration during
 * rapid show/hide cycles.
 * </p>
 *
 * @author XDSSWAR
 * @version 1.0.5
 * @since 1.0.0
 * @see NfxStage
 * @see NfxUtil
 * @see CornerPreference
 */
public class NfxWindow extends Stage {

    /*
     * Property holding the NfxUtil instance for this window.
     * The NfxUtil instance provides access to native DWM APIs for customizing the window.
     * It is created lazily when the window is first shown, as a native window handle is
     * required for its construction.
     */
    private final ObjectProperty<NfxUtil> nfxUtil;

    /**
     * Flag indicating whether the window has been shown at least once.
     * <p>
     * This flag is set to {@code true} when the {@code WINDOW_SHOWING} event fires for
     * the first time. It can be used by subclasses to differentiate between the initial
     * show and subsequent show operations after hide.
     * </p>
     * <p>
     * Marked as volatile to ensure visibility across threads, though access should
     * primarily occur on the JavaFX Application Thread.
     * </p>
     */
    protected volatile boolean firstShowAlready = false;

    /**
     * Flag tracking whether DWM property listeners have been registered.
     * <p>
     * <strong>FIX: Issue #7</strong> - Prevents duplicate listener registration when
     * WINDOW_SHOWING fires multiple times (e.g., show/hide/show cycles).
     * </p>
     * <p>
     * <strong>FIX: Issue #23</strong> - Access protected by {@link #listenerLock} using
     * synchronized blocks instead of volatile, because the check-then-act pattern
     * (check if false, then set to true) requires atomic operation.
     * </p>
     */
    private boolean propertyListenersAdded = false;

    /**
     * Lock object for thread-safe access to {@link #propertyListenersAdded}.
     * <p>
     * <strong>FIX: Issue #23</strong> - Volatile alone is insufficient for the check-then-act
     * pattern used during listener registration. This lock ensures atomicity.
     * </p>
     */
    private final Object listenerLock = new Object();

    /**
     * Reference to the initialization delay timer.
     * <p>
     * <strong>FIX: Issue #24</strong> - Stored as an instance field to prevent garbage
     * collection before the onFinished callback executes. Previously, a local variable
     * reference could be collected by GC, causing the callback to never fire and breaking
     * the initialization sequence.
     * </p>
     * <p>
     * The 3ms delay ensures the native window is fully created before DWM APIs are called.
     * </p>
     */
    private PauseTransition initPauseTransition;

    /*
     * =================================================================================================================
     *                                    Cached Listener References
     *
     * FIX: Issue #7 - Store listener references to enable proper cleanup and prevent
     * duplicate registration. These listeners synchronize JavaFX property changes with
     * native DWM attributes.
     * =================================================================================================================
     */

    /** Listener for corner preference changes. */
    private InvalidationListener cornerPrefListener;

    /** Listener for border color changes. */
    private ChangeListener<Color> borderColorListener;

    /** Listener for width changes (triggers layout recalculation). */
    private ChangeListener<Number> widthListener;

    /** Listener for height changes (triggers layout recalculation). */
    private ChangeListener<Number> heightListener;

    /** Listener for title bar color changes. */
    private ChangeListener<Color> titleBarColorListener;

    /** Listener for caption (title text) color changes. */
    private ChangeListener<Color> captionColorListener;

    /**
     * Event handler for the WINDOW_SHOWING event that initializes native DWM integration.
     * <p>
     * This handler is invoked each time the window is about to be shown. It performs the
     * following initialization steps:
     * </p>
     * <ol>
     *   <li>Sets the {@link #firstShowAlready} flag</li>
     *   <li>Schedules a delayed initialization (3ms) to ensure native window exists</li>
     *   <li>Creates the {@link NfxUtil} instance with the native window handle</li>
     *   <li>Applies initial DWM attribute values</li>
     *   <li>Registers property change listeners (only once, guarded by {@link #propertyListenersAdded})</li>
     * </ol>
     *
     * <p>
     * <strong>FIX: Issue #7</strong> - Listener registration is now guarded to prevent duplicates.
     * The WINDOW_SHOWING event can fire multiple times during show/hide/show cycles.
     * </p>
     */
    private final EventHandler<WindowEvent> LISTENER = windowEvent -> {
        firstShowAlready = true;
        /*
         * FIX: Issue #24 - Store PauseTransition in field to prevent GC before callback.
         * The brief delay helps avoid flicker during window show.
         */
        initPauseTransition = new PauseTransition(Duration.millis(3));
        initPauseTransition.setOnFinished(event -> {
            ensureNfx();

            // FIX: Issue #10 - Add null check for getNfxUtil()
            NfxUtil nfxUtil = getNfxUtil();
            if (nfxUtil == null) {
                return;
            }

            if (getTitleBarColor()!=null) {
                nfxUtil.setTitleBarColor(getTitleBarColor());
            }

            if (getCaptionColor() != null){
                nfxUtil.setCaptionColor(getCaptionColor());
            }

            nfxUtil.setCornerPref(getCornerPreference());
            nfxUtil.setBorderColor(getWindowBorder());

            /*
             * FIX: Issue #7 & #23 - Only add listeners once, with proper synchronization.
             * This prevents listener accumulation and duplicate callbacks.
             * Using synchronized block instead of volatile for thread-safe check-then-act.
             */
            synchronized (listenerLock) {
                if (propertyListenersAdded) {
                    return; // Already added, exit early
                }
                propertyListenersAdded = true;
            }

            // Store references so listeners can be identified/removed if needed
            cornerPrefListener = observable -> {
                NfxUtil util = getNfxUtil();
                if (util != null) {
                    util.setCornerPref(getCornerPreference());
                    invalidateSpots();
                }
            };
            cornerPreferenceProperty().addListener(cornerPrefListener);

            borderColorListener = (obs1, o1, border) -> {
                NfxUtil util = getNfxUtil();
                if (util != null) {
                    util.setBorderColor(border);
                    invalidateSpots();
                }
            };
            windowBorderProperty().addListener(borderColorListener);

            widthListener = (obs, o, n) -> {
                update(isMaximized(), isFullScreen());
                invalidateSpots();
            };
            widthProperty().addListener(widthListener);

            heightListener = (obs, o, n) -> {
                update(isMaximized(), isFullScreen());
                invalidateSpots();
            };
            heightProperty().addListener(heightListener);

            titleBarColorListener = (obs, o, color) -> {
                // FIX: Issue #10 - Consistent null checking
                NfxUtil util = getNfxUtil();
                if (color != null && util != null) {
                    util.setTitleBarColor(color);
                }
            };
            titleBarColorProperty().addListener(titleBarColorListener);

            captionColorListener = (obs, o, color) -> {
                // FIX: Issue #10 - Consistent null checking
                NfxUtil util = getNfxUtil();
                if (color != null && util != null) {
                    util.setCaptionColor(color);
                }
            };
            captionColorProperty().addListener(captionColorListener);
        });
        // FIX: Issue #24 - Use field reference instead of local variable
        initPauseTransition.play();
    };

    /**
     * Constructs a new NfxWindow with default settings.
     * <p>
     * The constructor initializes the internal state and registers the WINDOW_SHOWING
     * event handler for native DWM integration. Actual native initialization is deferred
     * until the window is shown.
     * </p>
     */
    public NfxWindow() {
        super();
        nfxUtil = new SimpleObjectProperty<>(this, "nfxUtil", null);
        initialize();
    }

    /**
     * Performs initial setup of the NfxWindow.
     * <p>
     * Registers the WINDOW_SHOWING event handler that will initialize native DWM
     * functionality when the window is first shown. This method is called from the
     * constructor.
     * </p>
     * <p>
     * On non-Windows platforms, this method does nothing, allowing NfxWindow to be
     * used as a standard Stage with no side effects.
     * </p>
     */
    private void initialize() {
        if (NfxUtil.isWindows()) {
            addEventHandler(WindowEvent.WINDOW_SHOWING, LISTENER);
        }
    }


    /**
     * Returns the property containing the NfxUtil instance for this window.
     * <p>
     * The property value is {@code null} until the window is shown and native
     * initialization completes. Subclasses can observe this property to detect
     * when native functionality becomes available.
     * </p>
     *
     * @return the NfxUtil property (never null, but may contain null value)
     */
    protected final ObjectProperty<NfxUtil> nfxUtilProperty() {
        return nfxUtil;
    }

    /**
     * Returns the current NfxUtil instance, or {@code null} if not yet initialized.
     * <p>
     * The NfxUtil instance is created when the window is first shown. Callers should
     * check for null before use, or ensure they are calling after the window is shown.
     * </p>
     *
     * @return the NfxUtil instance, or {@code null} if the window hasn't been shown yet
     */
    protected NfxUtil getNfxUtil() {
        return nfxUtilProperty().get();
    }


    /**
     * Ensures the NfxUtil instance is initialized.
     * <p>
     * If the NfxUtil instance doesn't exist, this method creates it. This is typically
     * called automatically during window showing, but can be called manually if needed.
     * </p>
     * <p>
     * <strong>Note:</strong> This method requires the window to be shown (realized) to
     * successfully create the NfxUtil instance, as a native window handle is required.
     * </p>
     */
    protected void ensureNfx() {
        if (getNfxUtil() == null) {
            nfxUtilProperty().set(new NfxUtil(this));
        }
    }

    /**
     * Resets the NfxUtil instance to {@code null}.
     * <p>
     * This can be called when the window is hidden or closed to release native resources.
     * A new NfxUtil instance will be created the next time the window is shown.
     * </p>
     */
    protected void resetNfx() {
        nfxUtilProperty().set(null);
    }

    /*
     * =================================================================================================================
     *
     *                                    Title Bar Color Property
     *
     * Controls the background color of the window's title bar via DWMWA_CAPTION_COLOR.
     * =================================================================================================================
     */

    // Property holding the title bar background color.
    // Changes to this property are automatically synchronized with the native DWM attribute
    // when the window is shown.
    private ObjectProperty<Color> titleBarColor;

    /**
     * Returns the observable property for the title bar color.
     * <p>
     * The property is lazy-initialized on first access. Binding or listening to this
     * property allows reactive updates to the window appearance.
     * </p>
     *
     * @return the title bar color property (never null)
     */
    public final ObjectProperty<Color> titleBarColorProperty() {
        if (titleBarColor == null) {
            titleBarColor = new SimpleObjectProperty<>(this, "titleBarColor");
        }
        return titleBarColor;
    }

    /**
     * Gets the current title bar background color.
     *
     * @return the title bar color, or {@code null} if not set
     */
    public final Color getTitleBarColor() {
        return titleBarColorProperty().get();
    }

    /**
     * Sets the title bar background color.
     * <p>
     * The color is applied to the native window via DWM if the window is currently shown.
     * If the window is not yet shown, the color will be applied when it becomes visible.
     * </p>
     *
     * @param titleBarColor the desired title bar color, or {@code null} for system default
     */
    public final void setTitleBarColor(Color titleBarColor) {
        titleBarColorProperty().set(titleBarColor);
    }

    /**
     * Sets the title bar background color using a hex color string.
     *
     * @param titleBarColor hex color string (e.g., "#1a1a2e" or "1a1a2e")
     * @see NfxUtil#hexToColor(String)
     */
    public final void setTitleBarColor(String titleBarColor) {
        setTitleBarColor(NfxUtil.hexToColor(titleBarColor));
    }


    /*
     * =================================================================================================================
     *
     *                                    Caption (Title Text) Color Property
     *
     * Controls the color of the window title text via DWMWA_TEXT_COLOR.
     * =================================================================================================================
     */

    // Property holding the caption (title text) color.
    private ObjectProperty<Color> captionColor;

    /**
     * Returns the observable property for the caption (title text) color.
     * <p>
     * The property is lazy-initialized on first access.
     * </p>
     *
     * @return the caption color property (never null)
     */
    public final ObjectProperty<Color> captionColorProperty() {
        if (captionColor == null) {
            captionColor = new SimpleObjectProperty<>(this, "captionColor");
        }
        return captionColor;
    }

    /**
     * Gets the current caption (title text) color.
     *
     * @return the caption color, or {@code null} if not set
     */
    public final Color getCaptionColor() {
        return captionColorProperty().get();
    }

    /**
     * Sets the caption (title text) color.
     * <p>
     * For best readability, choose a color with good contrast against the title bar
     * background color.
     * </p>
     *
     * @param captionColor the desired caption color, or {@code null} for system default
     */
    public final void setCaptionColor(Color captionColor) {
        captionColorProperty().set(captionColor);
    }


    /**
     * Sets the caption (title text) color using a hex color string.
     *
     * @param htmlColor hex color string (e.g., "#ffffff" or "ffffff")
     * @see NfxUtil#hexToColor(String)
     */
    public final void setCaptionColor(String htmlColor) {
        setCaptionColor(NfxUtil.hexToColor(htmlColor));
    }

    /*
     * =================================================================================================================
     *
     *                                    Corner Preference Property
     *
     * Controls window corner rounding style via DWMWA_WINDOW_CORNER_PREFERENCE.
     * This feature is primarily effective on Windows 11.
     * =================================================================================================================
     */

    // Property holding the window corner preference.
    private ObjectProperty<CornerPreference> cornerPreference;

    /**
     * Returns the observable property for the corner preference.
     * <p>
     * The property is lazy-initialized with a default value of {@link CornerPreference#DEFAULT}.
     * </p>
     *
     * @return the corner preference property (never null)
     */
    public final ObjectProperty<CornerPreference> cornerPreferenceProperty() {
        if (cornerPreference == null) {
            cornerPreference = new SimpleObjectProperty<>(this, "cornerPreference", CornerPreference.DEFAULT);
        }
        return cornerPreference;
    }

    /**
     * Gets the current corner preference for the window.
     *
     * @return the corner preference (never null, defaults to {@link CornerPreference#DEFAULT})
     */
    public final CornerPreference getCornerPreference() {
        return cornerPreferenceProperty().get();
    }

    /**
     * Sets the corner preference for the window.
     * <p>
     * On Windows 11, this controls whether corners are rounded and the radius of rounding.
     * On Windows 10 and earlier, this setting may have no visible effect.
     * </p>
     *
     * @param cornerPreference the desired corner style
     * @see CornerPreference
     */
    public final void setCornerPreference(CornerPreference cornerPreference) {
        this.cornerPreferenceProperty().set(cornerPreference);
    }

    /*
     * =================================================================================================================
     *
     *                                    Window Border Color Property
     *
     * Controls the window border color via DWMWA_BORDER_COLOR.
     * =================================================================================================================
     */

    // Property holding the window border color.
    private ObjectProperty<Color> windowBorder;

    /**
     * Returns the observable property for the window border color.
     * <p>
     * The property is lazy-initialized on first access with no default value (null).
     * </p>
     *
     * @return the window border color property (never null)
     */
    public final ObjectProperty<Color> windowBorderProperty() {
        if (windowBorder == null) {
            windowBorder = new SimpleObjectProperty<>(this, "windowBorder");
        }
        return windowBorder;
    }

    /**
     * Gets the current window border color.
     *
     * @return the border color, or {@code null} for system default
     */
    public final Color getWindowBorder() {
        return windowBorderProperty().get();
    }

    /**
     * Sets the window border color.
     * <p>
     * The border is the thin line around the window edge. On Windows 11 with rounded
     * corners, this border is always visible (1 pixel). Setting to {@code null} uses
     * the system default border color.
     * </p>
     *
     * @param windowBorder the desired border color, or {@code null} for system default
     */
    public final void setWindowBorder(Color windowBorder) {
        this.windowBorderProperty().set(windowBorder);
    }

    /*
     * =================================================================================================================
     *
     *                                    Protected Extension Points
     *
     * These methods are designed to be overridden by subclasses (e.g., NfxStage) to
     * respond to state changes that affect hit-testing or window layout.
     * =================================================================================================================
     */

    /**
     * Called when hit spot regions need to be recalculated.
     * <p>
     * This method is invoked when properties change that may affect the location or
     * validity of hit-testable regions (e.g., corner preference, border color, window size).
     * </p>
     * <p>
     * The default implementation does nothing. Subclasses like {@link NfxStage} override
     * this to mark their hit spot cache as dirty and schedule recalculation.
     * </p>
     */
    protected void invalidateSpots() {
    }

    /**
     * Called when the window state changes (maximized or fullscreen).
     * <p>
     * This method is invoked when the window's maximized or fullscreen state changes,
     * allowing subclasses to adjust their layout or behavior accordingly.
     * </p>
     * <p>
     * The default implementation does nothing.
     * </p>
     *
     * @param max  {@code true} if the window is now maximized
     * @param full {@code true} if the window is now in fullscreen mode
     */
    protected void update(boolean max, boolean full) {
    }
}
