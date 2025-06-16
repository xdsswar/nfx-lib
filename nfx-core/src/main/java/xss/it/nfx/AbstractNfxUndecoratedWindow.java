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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.WindowEvent;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author XDSSWAR
 * Created on 04/16/2024
 */
public abstract class AbstractNfxUndecoratedWindow extends NfxWindow {
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
     * EventType for the background change event.
     */
    public static final EventType<WindowEvent> BACKGROUND_CHANGE = new EventType<>(Event.ANY, "BACKGROUND_CHANGE");

    /**
     * Custom timer to handel hitSpot events
     */
    private Timer timer;

    /**
     * HitSpots
     */
    private final List<HitSpot> HIT_SPOTS;

    /**
     * Prev WindowState
     */
    protected WindowState prevState = null;


    /**
     * Constructs a new AbstractNfxUndecoratedWindow with default settings.
     * Calls the constructor with the parameter 'hideFromTaskBar' set to false.
     */
    public AbstractNfxUndecoratedWindow() {
        this(false);
    }

    /**
     * Constructs a new AbstractNfxUndecoratedWindow with the specified hideFromTaskBar setting.
     * Calls the super constructor, initializes the window, and sets the hideFromTaskBar property.
     *
     * @param hideFromTaskBar Indicates whether the window should be hidden from the taskbar.
     */
    public AbstractNfxUndecoratedWindow(boolean hideFromTaskBar){
        super();
        HIT_SPOTS = new CopyOnWriteArrayList<>();
        initialize();
        setHideFromTaskBar(hideFromTaskBar);
    }

    /**
     * Initializes the AbstractNfxUndecoratedWindow.
     * This method performs any necessary initialization steps for the window.
     */
    private void initialize(){
        nfxUtilProperty().addListener((obs, o, nfxUtil) -> {
            if (nfxUtil != null){
                install(nfxUtil.getHWnd());
                update(isMaximized(), isFullScreen());
                hideFromTaskBar(nfxUtil.getHWnd(),getHideFromTaskBar());
                hideFromTaskBarProperty().addListener((obs1, o1, hide)
                        -> hideFromTaskBar(nfxUtil.getHWnd(), hide));

                if (isMaximized()) setWindowState(WindowState.MAXIMIZED);
                maximizedProperty().addListener((obs1, o1, max) -> {
                    if (max) {
                        setWindowState(WindowState.MAXIMIZED);
                    }
                    else {
                        if (!isFullScreen() && !isIconified()){
                            setWindowState(WindowState.NORMAL);
                        }
                    }
                    invalidateSpots();
                    updateHitSpots();
                });

                if (isFullScreen()) setWindowState(WindowState.FULL_SCREEN);
                fullScreenProperty().addListener((obs1, o1, full) -> {
                    if (full) {
                        setWindowState(WindowState.FULL_SCREEN);
                    }
                    else {
                        if (!isMaximized() && !isIconified()){
                            setWindowState(WindowState.NORMAL);
                        }
                    }
                    invalidateSpots();
                    updateHitSpots();
                });

                if (isIconified()) setWindowState(WindowState.MINIMIZED);

                iconifiedProperty().addListener((obs1, o1, min) -> {
                    if (min) {
                        setWindowState(WindowState.MINIMIZED);
                    }
                    else {
                        if (prevState != null){
                            setWindowState(prevState);
                        }
                    }
                    invalidateSpots();
                    updateHitSpots();
                });

                //State
                handleState(getWindowState());

                windowStateProperty().addListener((ob, old, state) -> {
                    if (WindowState.MINIMIZED != old){
                        prevState = old;
                    }
                    handleState(state);
                });

                cornerPreferenceProperty().addListener(observable -> {
                    invalidateSpots();
                    update(isMaximized(), isFullScreen());
                    updateHitSpots();
                });

                windowBorderProperty().addListener((observableValue, color, t1) -> {
                    invalidateSpots();
                    update(isMaximized(), isFullScreen());
                    updateHitSpots();
                });

                sceneProperty().addListener((obs1, scene, s) -> refresh());
                updateHitSpots();
            }
        });
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
    public final ObjectProperty<Color> windowBackgroundProperty() {
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
    public final Color getWindowBackground() {
        return windowBackgroundProperty().get();
    }

    /**
     * Sets the window's background color.
     *
     * @param windowBackground The background color to set.
     */
    public final void setWindowBackground(Color windowBackground) {
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
    public final void update(){
        refresh();
    }

    /**
     * Updates the hit spots in the window.
     */
    private void updateHitSpots(){
        EventQueue.invokeLater( () -> {
            if( timer != null ) {
                timer.restart();
                return;
            }

            timer = new Timer( 300, e -> {
                timer = null;
                HIT_SPOTS.clear();
                HIT_SPOTS.addAll(getHitSpots());
            } );
            timer.setRepeats( false );
            timer.start();
        } );
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
            case MAXIMIZED -> setMaximized(true);
            case MINIMIZED -> setIconified(true);
            case FULL_SCREEN -> setFullScreen(true);
        }

        update(isMaximized(), isFullScreen());
        invalidateSpots();
        updateHitSpots();
    }


    /**
     * Triggers an update to refresh the entire window and its spots
     */
    public final void refresh(){
        invalidateSpots();
        updateHitSpots();
    }

    /**
     * Gets a list of HitSpot objects associated with this object.
     *
     * @return A list of HitSpot objects.
     */
    public abstract List<HitSpot> getHitSpots();


    /**
     * Gets the height of the title bar.
     *
     * @return The height of the title bar.
     */
    public abstract double getTitleBarHeight();


    /**
     * Updates the window state based on the provided parameters.
     *
     * @param max  Whether the window should be maximized.
     * @param full Whether the window should be in full-screen mode.
     */
    protected final void update(boolean max, boolean full) {
        update(getNfxUtil().getHWnd(), isMaximized(), isFullScreen());
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
     *                                         JNI callables
     *
     * =================================================================================================================
     */
    private HitSpot currentHoveredSpot = null;

    /**
     * Handles the non-client hit test for the given point (x, y) and resize border flag. Call from JNI
     *
     * @param x                 The x-coordinate of the point.
     * @param y                 The y-coordinate of the point.
     * @param isOnResizeBorder  A boolean flag indicating whether the point is on a resize border.
     * @return The hit test result code.
     */
    private int jniHitTest(int x, int y, boolean isOnResizeBorder ) {
        Point2D pt = scaleDown(new Point2D(x, y));
        int sx = (int) pt.getX();
        int sy = (int) pt.getY();

        boolean isOnTitleBar = sy < (getWindowState() == WindowState.MAXIMIZED ? (getTitleBarHeight() + 5) : getTitleBarHeight());
        HitSpot newHoveredSpot = null; // Track the new spot being hovered

        if (isOnTitleBar) {
            for (HitSpot spot : HIT_SPOTS) {
                if (contains(spot.getRect(), sx, sy)) {
                    newHoveredSpot = spot;
                    break; // Found the hovered spot, exit loop
                }
            }

            // Only update if the hovered spot has changed
            if (currentHoveredSpot != newHoveredSpot) {
                invalidateSpots(); // Invalidate all spots

                if (newHoveredSpot != null) {
                    newHoveredSpot.setHovered(true); // Set the new hovered spot
                }

                currentHoveredSpot = newHoveredSpot; // Update current hovered spot reference
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
            currentHoveredSpot = null;
        }
        invalidateSpots();
        // Return based on title bar and resize border status
        return isOnTitleBar ? (isOnResizeBorder ? HT_TOP : HT_CAPTION) : (isOnResizeBorder ? HT_TOP : HT_CLIENT);

    }


    /**
     * Checks if the window is in fullscreen mode. Call from JNI
     *
     * @return True if the window is in fullscreen mode, false otherwise.
     */
    private boolean jniIsFullScreen(){
        return isFullScreen();
    }

    /**
     * Checks if the window is maximized using JNI.
     *
     * @return True if the window is maximized, false otherwise
     */
    private boolean jniIsMaximized() {
        return isMaximized();
    }


    /**
     * Fires a state change event. Call from JNI.
     * This method triggers the firing of a custom event to indicate a state change.
     */
    private void jniFireStateChanged(){
        fireEvent(new Event(BACKGROUND_CHANGE));
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
     * Scales down the given Point2D object based on the output scale of the current Screen.
     *
     * @param point The Point2D object to scale down.
     * @return The scaled down Point2D object.
     */
    private Point2D scaleDown(Point2D point) {
        Screen screen= Rect.getCurrentScreen(this);
        double scaleX = screen.getOutputScaleX();
        double scaleY = screen.getOutputScaleY();
        double scaledX = point.getX() / scaleX;
        double scaledY = point.getY() / scaleY;
        return new Point2D(scaledX, scaledY);
    }

    /**
     * Rounds the given double value and returns it as an integer, clipping it to stay within the range of Integer.MIN_VALUE and Integer.MAX_VALUE.
     *
     * @param value The double value to round.
     * @return The rounded integer value, clipped to the range of Integer.MIN_VALUE and Integer.MAX_VALUE.
     */
    @Deprecated
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
    protected final void invalidateSpots(){
        HIT_SPOTS.forEach(hitSpot -> hitSpot.setHovered(false));
    }

}
