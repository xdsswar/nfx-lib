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
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base class for undecorated JavaFX windows with native Windows integration.
 * <p>
 * This class provides the core infrastructure for creating custom window chrome that
 * integrates properly with the Windows operating system. It handles:
 * </p>
 * <ul>
 *   <li><strong>Window State Management:</strong> Synchronization between JavaFX and native window states</li>
 *   <li><strong>Hit Testing:</strong> Native WM_NCHITTEST message handling for custom title bar interaction</li>
 *   <li><strong>Taskbar Integration:</strong> Optional hiding of windows from the taskbar</li>
 *   <li><strong>JNI Callbacks:</strong> Thread-safe handling of native-to-Java callbacks</li>
 *   <li><strong>Lifecycle Management:</strong> Proper installation/uninstallation of native hooks</li>
 * </ul>
 *
 * <h2>Native Integration Architecture</h2>
 * <p>
 * The class works by replacing the window's WndProc (window procedure) with a custom one
 * that intercepts Windows messages like WM_NCCALCSIZE and WM_NCHITTEST. This enables:
 * </p>
 * <pre>
 *     ┌─────────────────┐        ┌──────────────────┐
 *     │ Windows OS      │        │ JavaFX Stage     │
 *     │ (native msgs)   │        │ (JNI callbacks)  │
 *     └────────┬────────┘        └────────┬─────────┘
 *              │                          │
 *              │  WM_NCHITTEST            │
 *              ├─────────────────────────▶│ jniHitTest()
 *              │                          │
 *              │  WM_NCCALCSIZE           │
 *              ├─────────────────────────▶│ (handled natively)
 *              │                          │
 *              │  SC_MAXIMIZE             │
 *              ├─────────────────────────▶│ state sync
 *              │                          │
 * </pre>
 *
 * <h2>Window State Synchronization</h2>
 * <p>
 * Window state (normal, maximized, minimized, fullscreen) is tracked in both JavaFX properties
 * and native Windows state. This class keeps them synchronized:
 * </p>
 * <ul>
 *   <li>JavaFX {@code maximizedProperty()} ↔ Native {@code WS_MAXIMIZE}</li>
 *   <li>JavaFX {@code iconifiedProperty()} ↔ Native {@code WS_MINIMIZE}</li>
 *   <li>{@link #windowStateProperty()} provides unified state tracking</li>
 * </ul>
 *
 * <h2>JNI Thread Safety</h2>
 * <p>
 * Native Windows messages are processed on the Windows message pump thread, not the JavaFX
 * Application Thread. This class handles thread safety by:
 * </p>
 * <ul>
 *   <li>Using volatile fields for cross-thread visibility</li>
 *   <li>Using {@link AtomicReference} for complex state</li>
 *   <li>Deferring JavaFX property modifications via {@code Platform.runLater()}</li>
 *   <li>Caching frequently-accessed values for lock-free native access</li>
 * </ul>
 *
 * <h2>CSS Pseudo-Classes</h2>
 * <p>
 * Static pseudo-class definitions are provided for styling title bar controls based on
 * native hover state:
 * </p>
 * <table border="1">
 *   <caption>Available Pseudo-Classes</caption>
 *   <tr><th>Constant</th><th>CSS Selector</th><th>Purpose</th></tr>
 *   <tr><td>{@link #HT_CLOSE_CLASS}</td><td>{@code :ht-close}</td><td>Close button hover</td></tr>
 *   <tr><td>{@link #HT_MAX_CLASS}</td><td>{@code :ht-max}</td><td>Maximize button hover</td></tr>
 *   <tr><td>{@link #HT_MIN_CLASS}</td><td>{@code :ht-min}</td><td>Minimize button hover</td></tr>
 *   <tr><td>{@link #HT_CLIENT_CLASS}</td><td>{@code :ht-client}</td><td>Client area hover</td></tr>
 * </table>
 *
 * <h2>Subclass Requirements</h2>
 * <p>
 * Concrete subclasses must implement:
 * </p>
 * <ul>
 *   <li>{@link #getHitSpots()} - Return the list of active HitSpot regions</li>
 *   <li>{@link #getTitleBarHeight()} - Return the height of the custom title bar</li>
 * </ul>
 *
 * @author XDSSWAR
 * @version 1.0.5
 * @since 1.0.0
 * @see NfxWindow
 * @see NfxStage
 * @see HitSpot
 * @see WindowState
 */
public abstract class AbstractNfxUndecoratedWindow extends NfxWindow {

    /*
     * =================================================================================================================
     *
     *                                   Win32 Hit-Test Constants (HTCODE)
     *
     * These values are returned from WM_NCHITTEST and tell Windows how to handle mouse events
     * in that region. They must match the values defined in WinUser.h.
     *
     * @see <a href="https://docs.microsoft.com/en-us/windows/win32/inputdev/wm-nchittest">
     *      WM_NCHITTEST (Microsoft Docs)</a>
     * =================================================================================================================
     */

    /**
     * Hit-test code for client area (HTCLIENT).
     * <p>
     * Mouse events in this region are forwarded to the window and handled normally.
     * Used for interactive controls within the custom title bar.
     * </p>
     */
    private static final int HT_CLIENT = 1;

    /**
     * Hit-test code for title bar/caption area (HTCAPTION).
     * <p>
     * Mouse events in this region trigger window dragging behavior. Clicking and
     * dragging moves the window, double-clicking toggles maximize state.
     * </p>
     */
    private static final int HT_CAPTION = 2;

    /**
     * Hit-test code for system menu icon (HTSYSMENU).
     * <p>
     * Clicking this region opens the system menu (Move, Size, Minimize, etc.).
     * Double-clicking closes the window.
     * </p>
     */
    private static final int HT_SYS_MENU = 3;

    /**
     * Hit-test code for minimize button (HTMINBUTTON).
     * <p>
     * Clicking this region minimizes the window to the taskbar.
     * </p>
     */
    private static final int HT_MIN_BUTTON = 8;

    /**
     * Hit-test code for maximize button (HTMAXBUTTON).
     * <p>
     * Clicking this region toggles between maximized and normal states. On Windows 11,
     * hovering over this region displays the Snap Layouts flyout menu.
     * </p>
     */
    private static final int HT_MAX_BUTTON = 9;

    /**
     * Hit-test code for top window edge (HTTOP).
     * <p>
     * Used for the top resize border. The cursor changes to vertical resize and
     * dragging resizes the window vertically.
     * </p>
     */
    private static final int HT_TOP = 12;

    /**
     * Hit-test code for close button (HTCLOSE).
     * <p>
     * Clicking this region closes the window. This is a Windows 2000+ extension.
     * </p>
     */
    private static final int HT_CLOSE = 20;

    /*
     * =================================================================================================================
     *
     *                                        Custom Events
     *
     * =================================================================================================================
     */

    /**
     * Event type fired when the window's background state changes.
     * <p>
     * This event is fired from native code when the window activation state changes or
     * when the system theme changes. Applications can listen for this event to update
     * their UI accordingly.
     * </p>
     *
     * <h4>Usage Example</h4>
     * <pre>{@code
     * nfxStage.addEventHandler(AbstractNfxUndecoratedWindow.BACKGROUND_CHANGE, event -> {
     *     // Update UI based on activation state
     *     updateTheme();
     * });
     * }</pre>
     */
    public static final EventType<WindowEvent> BACKGROUND_CHANGE = new EventType<>(Event.ANY, "BACKGROUND_CHANGE");


    /*
     * =================================================================================================================
     *
     *                                    Thread-Safe State Management
     *
     * These fields are accessed from both the JavaFX Application Thread and the native Windows
     * message thread. They use appropriate synchronization mechanisms for thread safety.
     * =================================================================================================================
     */

    /**
     * Thread-safe reference to the current list of HitSpots for native hit-testing.
     * <p>
     * <strong>FIX: Issue #3</strong> - Using AtomicReference ensures atomic list replacement.
     * The previous implementation using clear+addAll could expose an empty list to readers
     * between the two operations. Now, readers always see either the old complete list or
     * the new complete list, never an intermediate state.
     * </p>
     * <p>
     * The contained list is always immutable (created via {@code List.copyOf()}).
     * </p>
     */
    private final AtomicReference<List<HitSpot>> HIT_SPOTS = new AtomicReference<>(List.of());

    /**
     * Tracks the previous window state before minimization.
     * <p>
     * When the window is restored from minimized state, it should return to this state
     * (either NORMAL or MAXIMIZED), not always to NORMAL.
     * </p>
     * <p>
     * <strong>FIX: Issue #30</strong> - Made volatile for thread-safe access from the
     * iconifiedListener which may observe changes from native state updates.
     * </p>
     */
    protected volatile WindowState prevState = null;

    /*
     * =================================================================================================================
     *
     *                                    Listener References
     *
     * FIX: Issue #1 - Memory Leak Prevention
     *
     * Storing listener references allows proper cleanup when nfxUtil changes or when the window
     * is hidden. Without this, listeners would accumulate with each show/hide cycle, causing
     * memory leaks and potentially firing duplicate callbacks.
     * =================================================================================================================
     */

    /** Listener for hideFromTaskBar property changes. */
    private ChangeListener<Boolean> hideFromTaskBarListener;

    /** Listener for maximized property changes. */
    private ChangeListener<Boolean> maximizedListener;

    /** Listener for fullScreen property changes. */
    private ChangeListener<Boolean> fullScreenListener;

    /** Listener for iconified (minimized) property changes. */
    private ChangeListener<Boolean> iconifiedListener;

    /** Listener for windowState property changes. */
    private ChangeListener<WindowState> windowStateListener;

    /** Listener for cornerPreference property changes. */
    private InvalidationListener cornerPreferenceListener;

    /** Listener for windowBorder color property changes. */
    private ChangeListener<Color> windowBorderListener;

    /** Listener for scene property changes. */
    private ChangeListener<Scene> sceneListener;

    /** Listener for nfxUtil property changes (native initialization). */
    private ChangeListener<NfxUtil> nfxUtilListener;

    /*
     * =================================================================================================================
     *
     *                                    CSS Pseudo-Classes
     *
     * These pseudo-classes enable CSS styling based on native hover state. They are applied
     * to controls when the native hit-test indicates the mouse is over that region.
     * =================================================================================================================
     */

    /**
     * CSS pseudo-class for close button native hover state.
     * <p>
     * Applied to the close control when Windows reports the mouse is over the close
     * hit-test region. This may differ from JavaFX's own hover detection.
     * </p>
     *
     * <h4>CSS Usage</h4>
     * <pre>{@code
     * .close-button:ht-close {
     *     -fx-background-color: #e81123;  // Windows close button red
     * }
     * .close-button:ht-close .icon {
     *     -fx-fill: white;
     * }
     * }</pre>
     */
    protected static final PseudoClass HT_CLOSE_CLASS = PseudoClass.getPseudoClass("ht-close");

    /**
     * CSS pseudo-class for maximize/restore button native hover state.
     * <p>
     * Applied when Windows reports mouse hover over the maximize hit-test region.
     * On Windows 11, this region triggers the Snap Layouts flyout.
     * </p>
     *
     * <h4>CSS Usage</h4>
     * <pre>{@code
     * .max-button:ht-max {
     *     -fx-background-color: derive(-fx-base, 20%);
     * }
     * }</pre>
     */
    protected static final PseudoClass HT_MAX_CLASS = PseudoClass.getPseudoClass("ht-max");

    /**
     * CSS pseudo-class for minimize button native hover state.
     *
     * <h4>CSS Usage</h4>
     * <pre>{@code
     * .min-button:ht-min {
     *     -fx-background-color: derive(-fx-base, 20%);
     * }
     * }</pre>
     */
    protected static final PseudoClass HT_MIN_CLASS = PseudoClass.getPseudoClass("ht-min");

    /**
     * CSS pseudo-class for client area native hover state.
     * <p>
     * Applied to client area regions in the title bar when the native hit-test reports
     * HTCLIENT. Useful for styling interactive controls that should have different
     * appearance when receiving native hover.
     * </p>
     *
     * <h4>CSS Usage</h4>
     * <pre>{@code
     * .title-bar-button:ht-client {
     *     -fx-opacity: 1.0;
     * }
     * }</pre>
     */
    protected static final PseudoClass HT_CLIENT_CLASS = PseudoClass.getPseudoClass("ht-client");

    /*
     * =================================================================================================================
     *
     *                                    Debounce Timer
     *
     * =================================================================================================================
     */

    /**
     * Debounce timer for HitSpot snapshot updates.
     * <p>
     * When hit spots are invalidated (e.g., due to resize or control changes), this timer
     * prevents excessive snapshot rebuilds by coalescing multiple invalidations into a
     * single update after a 300ms delay.
     * </p>
     * <p>
     * The timer runs on the JavaFX Application Thread, ensuring snapshot updates are
     * properly synchronized with the UI.
     * </p>
     */
    private final PauseTransition hitSpotsDebounce = new PauseTransition(Duration.millis(300));

    /*
     * =================================================================================================================
     *
     *                                         Constructors
     *
     * =================================================================================================================
     */

    /**
     * Constructs a new undecorated window that appears in the Windows taskbar.
     * <p>
     * This is equivalent to calling {@code AbstractNfxUndecoratedWindow(false)}.
     * </p>
     */
    public AbstractNfxUndecoratedWindow() {
        this(false);
    }

    /**
     * Constructs a new undecorated window with configurable taskbar visibility.
     * <p>
     * The constructor performs the following initialization:
     * </p>
     * <ol>
     *   <li>Calls the parent {@link NfxWindow} constructor</li>
     *   <li>Sets up native integration listeners and event handlers</li>
     *   <li>Configures the debounce timer for HitSpot updates</li>
     *   <li>Applies the taskbar visibility setting (Windows only)</li>
     * </ol>
     * <p>
     * Native window hooks are installed later when the window is first shown, not during
     * construction, because a native window handle (HWND) is required.
     * </p>
     *
     * @param hideFromTaskBar {@code true} to hide from taskbar (useful for tool windows),
     *                        {@code false} to show normally in taskbar
     */
    public AbstractNfxUndecoratedWindow(boolean hideFromTaskBar) {
        super();
        initialize();
        if (NfxUtil.isWindows()) {
            setHideFromTaskBar(hideFromTaskBar);
        }
    }

    /**
     * Initializes the AbstractNfxUndecoratedWindow.
     * This method performs any necessary initialization steps for the window.
     * FIX: Issue #1 - Listeners are now stored in fields and properly removed during cleanup.
     */
    private void initialize(){
        if (NfxUtil.isWindows()) {
            /*
             * FIX: Issue #1 - Store the nfxUtil listener reference for cleanup.
             * Previous listeners are removed before adding new ones when nfxUtil changes.
             */
            nfxUtilListener = (obs, oldNfxUtil, nfxUtil) -> {
                // Remove old listeners if nfxUtil is being replaced
                if (oldNfxUtil != null) {
                    removePropertyListeners();
                }

                if (nfxUtil != null) {
                    install(nfxUtil.getHWnd());
                    update(isMaximized(), isFullScreen());
                    hideFromTaskBar(nfxUtil.getHWnd(), getHideFromTaskBar());

                    // FIX: Store listener reference for later removal
                    hideFromTaskBarListener = (obs1, o1, hide)
                            -> hideFromTaskBar(nfxUtil.getHWnd(), hide);
                    hideFromTaskBarProperty().addListener(hideFromTaskBarListener);

                    if (isMaximized()) setWindowState(WindowState.MAXIMIZED);

                    // FIX: Store listener reference for later removal
                    maximizedListener = (obs1, o1, max) -> {
                        if (max) {
                            setWindowState(WindowState.MAXIMIZED);
                        } else {
                            if (!isFullScreen() && !isIconified()) {
                                setWindowState(WindowState.NORMAL);
                            }
                        }
                        invalidateSpots();
                        updateHitSpots();
                    };
                    maximizedProperty().addListener(maximizedListener);

                    if (isFullScreen()) setWindowState(WindowState.FULL_SCREEN);

                    // FIX: Store listener reference for later removal
                    fullScreenListener = (obs1, o1, full) -> {
                        if (full) {
                            setWindowState(WindowState.FULL_SCREEN);
                        } else {
                            if (!isMaximized() && !isIconified()) {
                                setWindowState(WindowState.NORMAL);
                            }
                        }
                        invalidateSpots();
                        updateHitSpots();
                    };
                    fullScreenProperty().addListener(fullScreenListener);

                    if (isIconified()) setWindowState(WindowState.MINIMIZED);

                    // FIX: Store listener reference for later removal
                    iconifiedListener = (obs1, o1, min) -> {
                        if (min) {
                            setWindowState(WindowState.MINIMIZED);
                        } else {
                            if (prevState != null) {
                                setWindowState(prevState);
                            }
                        }
                        invalidateSpots();
                        updateHitSpots();
                    };
                    iconifiedProperty().addListener(iconifiedListener);

                    //State
                    handleState(getWindowState());

                    // FIX: Store listener reference for later removal
                    windowStateListener = (ob, old, state) -> {
                        if (WindowState.MINIMIZED != old) {
                            prevState = old;
                        }
                        handleState(state);
                    };
                    windowStateProperty().addListener(windowStateListener);

                    // FIX: Store listener reference for later removal
                    cornerPreferenceListener = observable -> {
                        invalidateSpots();
                        update(isMaximized(), isFullScreen());
                        updateHitSpots();
                    };
                    cornerPreferenceProperty().addListener(cornerPreferenceListener);

                    // FIX: Store listener reference for later removal
                    windowBorderListener = (observableValue, color, t1) -> {
                        invalidateSpots();
                        update(isMaximized(), isFullScreen());
                        updateHitSpots();
                    };
                    windowBorderProperty().addListener(windowBorderListener);

                    // FIX: Store listener reference for later removal
                    sceneListener = (obs1, scene, s) -> refresh();
                    sceneProperty().addListener(sceneListener);

                    updateHitSpots();
                }
            };
            nfxUtilProperty().addListener(nfxUtilListener);

            /*
             * FIX: Issue #31 - Set debounce handler once during initialization, not inside listener.
             * Previously, setOnFinished() was called every time nfxUtil changed, which could
             * accumulate handlers if nfxUtil changes multiple times.
             *
             * FIX: Issue #3 - Use atomic reference update instead of clear+addAll.
             * The debounce timer now atomically replaces the entire list.
             */
            hitSpotsDebounce.setOnFinished(e -> {
                // Atomically replace the entire list - readers see either old or new, never empty
                HIT_SPOTS.set(List.copyOf(getHitSpots()));
            });
        }

        addEventHandler(WindowEvent.WINDOW_SHOWN, e->{
            if (firstShowAlready) {
                updateOnStateLost();
            }
        });

        addEventHandler(WindowEvent.WINDOW_HIDDEN, e->{
            uninstall();
        });
    }

    /**
     * Removes all property listeners that were registered during initialization.
     * FIX: Issue #1 - This method is called during cleanup to prevent memory leaks
     * from accumulated listeners.
     */
    private void removePropertyListeners() {
        if (hideFromTaskBarListener != null) {
            hideFromTaskBarProperty().removeListener(hideFromTaskBarListener);
            hideFromTaskBarListener = null;
        }
        if (maximizedListener != null) {
            maximizedProperty().removeListener(maximizedListener);
            maximizedListener = null;
        }
        if (fullScreenListener != null) {
            fullScreenProperty().removeListener(fullScreenListener);
            fullScreenListener = null;
        }
        if (iconifiedListener != null) {
            iconifiedProperty().removeListener(iconifiedListener);
            iconifiedListener = null;
        }
        if (windowStateListener != null) {
            windowStateProperty().removeListener(windowStateListener);
            windowStateListener = null;
        }
        if (cornerPreferenceListener != null) {
            cornerPreferenceProperty().removeListener(cornerPreferenceListener);
            cornerPreferenceListener = null;
        }
        if (windowBorderListener != null) {
            windowBorderProperty().removeListener(windowBorderListener);
            windowBorderListener = null;
        }
        if (sceneListener != null) {
            sceneProperty().removeListener(sceneListener);
            sceneListener = null;
        }
    }

    /**
     * Property indicating whether the window should be shown in the taskbar.
     */
    private BooleanProperty hideFromTaskBar;

    /**
     * Returns the BooleanProperty representing whether the window should be hidden from taskbar.
     * If not already initialized, it creates a new BooleanProperty with a default value of true.
     *
     * @return The BooleanProperty for hideFromTaskBar.
     */
    public final BooleanProperty hideFromTaskBarProperty() {
        if (hideFromTaskBar == null) {
            hideFromTaskBar = new SimpleBooleanProperty(this, "hideFromTaskBar", false);
        }
        return hideFromTaskBar;
    }

    /**
     * Checks if the window should be shown in the taskbar.
     *
     * @return True if the window should be shown in the taskbar, false otherwise.
     */
    public final boolean getHideFromTaskBar() {
        return hideFromTaskBarProperty().get();
    }

    /**
     * Sets whether the window should be shown in the taskbar.
     *
     * @param hideFromTaskBar True to show the window in the taskbar, false to hide it.
     */
    public final void setHideFromTaskBar(boolean hideFromTaskBar) {
        this.hideFromTaskBarProperty().set(hideFromTaskBar);
    }


    /**
     * Property representing the window's background color.
     */
    private ObjectProperty<Color> windowBackground;

    /**
     * Returns the ObjectProperty representing the window's background color.
     * If not already initialized, it creates a new ObjectProperty with no default value.
     *
     * @return The ObjectProperty for windowBackground.
     */
    protected ObjectProperty<Color> windowBackgroundProperty() {
        if (windowBackground == null) {
            windowBackground = new SimpleObjectProperty<>(this, "windowBackground");
        }
        return windowBackground;
    }

    /**
     * Gets the window's background color.
     *
     * @return The window's background color.
     */
    protected Color getWindowBackground() {
        return windowBackgroundProperty().get();
    }

    /**
     * Sets the window's background color.
     *
     * @param windowBackground The background color to set.
     */
    protected void setWindowBackground(Color windowBackground) {
        this.windowBackgroundProperty().set(windowBackground);
    }

    /**
     * Property representing the state of the window.
     */
    private ObjectProperty<WindowState> windowState;

    /**
     * Returns the ObjectProperty representing the state of the window.
     * If not already initialized, it creates a new ObjectProperty with a default value of State.NORMAL.
     *
     * @return The ObjectProperty for windowState.
     */
    public final ObjectProperty<WindowState> windowStateProperty() {
        if (windowState == null) {
            windowState = new SimpleObjectProperty<>(this, "windowState", WindowState.NORMAL);
        }
        return windowState;
    }

    /**
     * Gets the state of the window.
     *
     * @return The window state.
     */
    public WindowState getWindowState() {
        return windowStateProperty().get();
    }

    /**
     * Sets the state of the window.
     *
     * @param windowState The window state to set.
     */
    public void setWindowState(WindowState windowState) {
        this.windowStateProperty().set(windowState);
    }

    /**
     * Updates the current state by invoking the {@code refresh} method.
     * <p>
     * This method provides a final update mechanism that calls {@code refresh} to refresh
     * the state or content, ensuring the most recent data or UI changes are applied.
     */
    protected void update(){
        refresh();
    }

    /**
     * Updates the hit spots in the window.
     */
    private void updateHitSpots(){
        if (Platform.isFxApplicationThread()) {
            hitSpotsDebounce.stop();
            hitSpotsDebounce.playFromStart();
        } else {
            Platform.runLater(() -> {
                hitSpotsDebounce.stop();
                hitSpotsDebounce.playFromStart();
            });
        }
    }

    /**
     * Handles the window state change.
     *
     * @param state The new window state
     */
    private void handleState(WindowState state){
        switch (state){
            case NORMAL -> {
                setMaximized(false);
                setFullScreen(false);
                setIconified(false);
            }
            case MAXIMIZED, FULL_SCREEN -> setMaximized(true);
            case MINIMIZED -> setIconified(true);
        }

        update(isMaximized(), isFullScreen());
        invalidateSpots();
        updateHitSpots();
    }


    /**
     * Triggers an update to refresh the entire window and its spots.
     * FIX: Issue #5 - Also updates cached title bar height for thread-safe JNI access.
     */
    public final void refresh(){
        if (NfxUtil.isWindows()) {
            updateCachedTitleBarHeight();
            invalidateSpots();
            updateHitSpots();
        }
    }


    /**
     * Called when the window state has been lost (e.g., after focus changes or OS-level events).
     * <p>
     * On Windows platforms, if a valid native window handle ({@code hwnd}) exists,
     * this method reinstalls the necessary hooks for the window. After handling
     * platform-specific reinstallation, it always triggers a {@link #refresh()}
     * to ensure the UI is updated.
     * </p>
     */
    private void updateOnStateLost(){
        if (NfxUtil.isWindows()) {
            ensureNfx();
            install(getNfxUtil().getHWnd());
        }
        refresh();
    }

    /**
     * Gets a list of HitSpot objects associated with this object.
     *
     * @return A list of HitSpot objects.
     */
    protected abstract List<HitSpot> getHitSpots();


    /**
     * Gets the height of the title bar.
     *
     * @return The height of the title bar.
     */
    protected abstract double getTitleBarHeight();


    /**
     * Updates the window state based on the provided parameters.
     *
     * @param max  Whether the window should be maximized.
     * @param full Whether the window should be in full-screen mode.
     */
    protected final void update(boolean max, boolean full) {
        if (NfxUtil.isWindows()) {
            update(getNfxUtil().getHWnd(), isMaximized(), isFullScreen());
        }
    }

    /**
     * Uninstall the system.
     * FIX: Issue #1 - Now also removes property listeners during cleanup to prevent memory leaks.
     */
    private void uninstall(){
        if (NfxUtil.isWindows()){
            // FIX: Remove all listeners before uninstalling
            removePropertyListeners();
            ensureNfx();
            uninstall(getNfxUtil().getHWnd());
            resetNfx();
        }
    }

    /*
     * =================================================================================================================
     */

    /**
     * Hides the window from the taskbar.
     *
     * @param hWnd The handle of the window to hide
     * @param hide True to hide the window from the taskbar, false to show it
     */
    private native void hideFromTaskBar(long hWnd, boolean hide);

    /**
     * This method will change the Window WinProc in the native side.
     * This is only for window that extends from AbstractNfxUndecoratedWindow, do not use it in another normal window
     *
     * @param hWnd The handle of the window to install
     */
    private native void install(long hWnd);

    /**
     * This method will change the Window WinProc in the native side.
     * This is only for window that extends from AbstractNfxUndecoratedWindow, do not use it in another normal window
     *
     * @param hWnd The handle of the window to install
     */
    private native void uninstall(long hWnd);


    /**
     * Native method to update window decorations for the window with the specified handle.
     *
     * @param hWnd The window handle.
     * @param maximized True if the window is maximized, false otherwise.
     * @param fullScreen is window is full screen
     */
    private native void update(long hWnd, boolean maximized, boolean fullScreen);


    /*
     * =================================================================================================================
     *
     *                                         JNI Callbacks
     *
     * These methods are called from the native Windows message handler thread in response to
     * Windows messages like WM_NCHITTEST, WM_ACTIVATE, etc. They must be thread-safe.
     *
     * IMPORTANT: All JNI callback methods must:
     *   1. Handle exceptions gracefully (FIX: Issue #6) - unhandled exceptions crash the JVM
     *   2. Not directly modify JavaFX properties - use Platform.runLater() (FIX: Issue #5)
     *   3. Use thread-safe data structures (volatile, AtomicReference, etc.)
     *   4. Return safe default values on error
     *
     * =================================================================================================================
     */

    /**
     * Currently hovered HitSpot for tracking hover state changes.
     * <p>
     * This is updated from the native thread during hit-testing and read when determining
     * whether to fire hover state change events. Marked volatile for cross-thread visibility.
     * </p>
     */
    private volatile HitSpot currentHoveredSpot = null;

    /**
     * Cached title bar height for thread-safe access from native code.
     * <p>
     * <strong>FIX: Issue #5</strong> - The native hit-test callback needs to know the title bar
     * height to determine if a point is in the title bar region. Since {@link #getTitleBarHeight()}
     * may access JavaFX properties that aren't thread-safe, we cache the value and update it
     * on the FX thread whenever the title bar height might change.
     * </p>
     * <p>
     * Default value of 40.0 provides a reasonable fallback before the first FX-thread update.
     * </p>
     */
    private volatile double cachedTitleBarHeight = 40.0;

    /**
     * Updates the cached title bar height from the current value.
     * <p>
     * This method should be called whenever the title bar height might have changed:
     * </p>
     * <ul>
     *   <li>After scene changes</li>
     *   <li>After window state changes (maximized/normal)</li>
     *   <li>After refresh() is called</li>
     * </ul>
     * <p>
     * If called from a non-FX thread, the update is deferred to the FX thread.
     * </p>
     */
    protected final void updateCachedTitleBarHeight() {
        if (Platform.isFxApplicationThread()) {
            cachedTitleBarHeight = getTitleBarHeight();
        } else {
            Platform.runLater(() -> cachedTitleBarHeight = getTitleBarHeight());
        }
    }

    /**
     * JNI callback for Windows WM_NCHITTEST message handling.
     * <p>
     * This method is invoked from the native layer when Windows needs to determine what part
     * of the window the mouse is over. The return value tells Windows how to handle mouse
     * events at that location (e.g., start a drag, show resize cursor, handle button click).
     * </p>
     *
     * <h4>Thread Safety</h4>
     * <p>
     * This method runs on the Windows message pump thread, NOT the JavaFX Application Thread.
     * It must not directly access or modify JavaFX properties. All fixes applied:
     * </p>
     * <ul>
     *   <li><strong>FIX: Issue #5</strong> - Uses {@link #cachedTitleBarHeight} instead of calling
     *       {@link #getTitleBarHeight()} which may access non-thread-safe JavaFX properties</li>
     *   <li><strong>FIX: Issue #5</strong> - Defers JavaFX property changes (hover states) to
     *       the FX thread via {@code Platform.runLater()}</li>
     *   <li><strong>FIX: Issue #6</strong> - Wrapped in try-catch to prevent JVM crash from
     *       unhandled exceptions in JNI callbacks</li>
     * </ul>
     *
     * <h4>Hit-Test Logic</h4>
     * <ol>
     *   <li>Check if point is in title bar region (y &lt; cachedTitleBarHeight)</li>
     *   <li>If in title bar, iterate through HitSpots to find matching region</li>
     *   <li>Return appropriate HTCODE (HTCLOSE, HTMAXBUTTON, HTMINBUTTON, HTCLIENT, HTCAPTION)</li>
     *   <li>If on resize border, return HTTOP to enable vertical resize</li>
     * </ol>
     *
     * @param x                the X coordinate in window client coordinates (DIP)
     * @param y                the Y coordinate in window client coordinates (DIP)
     * @param isOnResizeBorder {@code true} if the point is within the resize border region
     * @return Windows hit-test code (HT_CLIENT, HT_CAPTION, HT_CLOSE, HT_MAX_BUTTON, etc.)
     */
    private int jniHitTest(int x, int y, boolean isOnResizeBorder) {
        try {
            // FIX: Use cached title bar height instead of calling getTitleBarHeight() from non-FX thread
            boolean isOnTitleBar = y < cachedTitleBarHeight;
            HitSpot newHoveredSpot = null;

            if (isOnTitleBar) {
                // FIX: Issue #3 - Get atomic snapshot of HIT_SPOTS
                List<HitSpot> spots = HIT_SPOTS.get();

                for (HitSpot spot : spots) {
                    // FIX: Issue #4 - getRect() now has null checks internally
                    Rectangle2D rect = spot.getRectSafe();
                    if (rect != null && contains(rect, x, y)) {
                        newHoveredSpot = spot;
                        break;
                    }
                }

                // Only update if the hovered spot has changed
                if (currentHoveredSpot != newHoveredSpot) {
                    final HitSpot oldSpot = currentHoveredSpot;
                    final HitSpot spotToHover = newHoveredSpot;

                    // FIX: Issue #5 - Defer JavaFX property changes to FX thread
                    Platform.runLater(() -> {
                        invalidateSpots();
                        if (spotToHover != null) {
                            spotToHover.setHovered(true);
                        }
                    });

                    currentHoveredSpot = newHoveredSpot;
                }

                // Return appropriate value for the hovered spot type
                if (newHoveredSpot != null) {
                    if (newHoveredSpot.isSystemMenu()) return HT_SYS_MENU;
                    if (newHoveredSpot.isMinimize()) return HT_MIN_BUTTON;
                    if (newHoveredSpot.isMaximize()) return HT_MAX_BUTTON;
                    if (newHoveredSpot.isClose()) return HT_CLOSE;
                    if (newHoveredSpot.isClient()) return HT_CLIENT;
                }
            } else {
                if (currentHoveredSpot != null) {
                    currentHoveredSpot = null;
                    // FIX: Issue #5 - Defer to FX thread
                    Platform.runLater(this::invalidateSpots);
                }
            }

            // Return based on title bar and resize border status
            return isOnTitleBar ? (isOnResizeBorder ? HT_TOP : HT_CAPTION) : (isOnResizeBorder ? HT_TOP : HT_CLIENT);

        } catch (Exception e) {
            // FIX: Issue #6 - Exception handling prevents JVM crash
            // Return safe default on error
            return isOnResizeBorder ? HT_TOP : HT_CLIENT;
        }
    }


    /**
     * Checks if the window is in fullscreen mode. Call from JNI
     * FIX: Issue #6 - Added exception handling.
     *
     * @return True if the window is in fullscreen mode, false otherwise.
     */
    private boolean jniIsFullScreen() {
        try {
            return isFullScreen();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the window is maximized using JNI.
     * FIX: Issue #6 - Added exception handling.
     *
     * @return True if the window is maximized, false otherwise
     */
    private boolean jniIsMaximized() {
        try {
            return isMaximized();
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Fires a state change event. Call from JNI.
     * This method triggers the firing of a custom event to indicate a state change.
     * FIX: Issue #5 & #6 - Defer to FX thread and add exception handling.
     */
    private void jniFireStateChanged() {
        try {
            // FIX: Issue #5 - Must fire events on FX thread
            if (Platform.isFxApplicationThread()) {
                fireEvent(new Event(BACKGROUND_CHANGE));
            } else {
                Platform.runLater(() -> fireEvent(new Event(BACKGROUND_CHANGE)));
            }
        } catch (Exception e) {
            // FIX: Issue #6 - Silently handle to prevent JVM crash
        }
    }


    /**
     * Invalidates the hit spots by calling the invalidateSpots() method.
     * This method is invoked from the native side.
     * FIX: Issue #5 & #6 - Defer to FX thread and add exception handling.
     */
    private void jniInvalidateSpots() {
        try {
            // FIX: Issue #5 - Must modify JavaFX properties on FX thread
            if (Platform.isFxApplicationThread()) {
                invalidateSpots();
            } else {
                Platform.runLater(this::invalidateSpots);
            }
        } catch (Exception e) {
            // FIX: Issue #6 - Silently handle to prevent JVM crash
        }
    }

    /*
     * =================================================================================================================
     *
     *                                         JNI callables END
     *
     * =================================================================================================================
     */

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
     * Invalidates the hit spots by setting their hover state to false.
     * FIX: Issue #3 - Uses atomic get() to safely access the list.
     */
    protected final void invalidateSpots(){
        // FIX: Get atomic snapshot before iterating
        List<HitSpot> spots = HIT_SPOTS.get();
        for (HitSpot hitSpot : spots) {
            hitSpot.setHovered(false);
        }
    }
}
