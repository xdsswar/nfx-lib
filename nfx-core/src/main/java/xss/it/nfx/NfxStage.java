/*
 * Copyright © 2025. XTREME SOFTWARE SOLUTIONS
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

import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Control;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.WindowEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * High-level undecorated window stage with built-in title bar control management and hit-test caching.
 * <p>
 * {@code NfxStage} extends {@link AbstractNfxUndecoratedWindow} to provide a convenient API for
 * creating custom window chrome with proper Windows integration. It manages the mapping between
 * JavaFX controls and native hit-test regions, enabling features like:
 * </p>
 * <ul>
 *   <li>Windows 11 Snap Layouts on maximize button hover</li>
 *   <li>Native window dragging via title bar</li>
 *   <li>System menu integration</li>
 *   <li>Proper hover state synchronization via CSS pseudo-classes</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>
 * NfxStage uses a cached snapshot pattern for optimal hit-testing performance:
 * </p>
 * <pre>
 *     ┌─────────────┐    invalidate    ┌──────────────┐
 *     │ UI Changes  │ ───────────────▶ │ spotsDirty   │
 *     └─────────────┘                  └──────┬───────┘
 *                                             │
 *                                      rebuild on read
 *                                             │
 *                                             ▼
 *     ┌──────────────┐   atomic read   ┌──────────────┐
 *     │ Native Layer │ ◀───────────────│ hitSpotsSnapshot │
 *     └──────────────┘                 └──────────────┘
 * </pre>
 *
 * <h2>Control Registration</h2>
 * <p>
 * Register window controls (close, maximize, minimize) and client areas:
 * </p>
 * <pre>{@code
 * public class MyWindow extends NfxStage {
 *     private Button closeBtn, maxBtn, minBtn;
 *     private HBox titleBar;
 *
 *     @Override
 *     protected void initializeWindow() {
 *         // Register title bar controls
 *         setCloseControl(closeBtn);
 *         setMaxControl(maxBtn);
 *         setMinControl(minBtn);
 *
 *         // Register title bar as client area (enables drag)
 *         addClientAreas(titleBar);
 *     }
 * }
 * }</pre>
 *
 * <h2>CSS Pseudo-Classes</h2>
 * <p>
 * When native hover detection occurs, pseudo-classes are applied to the controls:
 * </p>
 * <table border="1">
 *   <caption>CSS Pseudo-Classes for Hit-Test States</caption>
 *   <tr><th>Pseudo-Class</th><th>Applied To</th><th>When</th></tr>
 *   <tr><td>{@code :ht-close}</td><td>Close control</td><td>Native hover over close region</td></tr>
 *   <tr><td>{@code :ht-max}</td><td>Maximize control</td><td>Native hover over maximize region</td></tr>
 *   <tr><td>{@code :ht-min}</td><td>Minimize control</td><td>Native hover over minimize region</td></tr>
 *   <tr><td>{@code :ht-client}</td><td>Client regions</td><td>Native hover over client area</td></tr>
 * </table>
 *
 * <pre>{@code
 * // CSS styling for native hover states
 * .close-button:ht-close {
 *     -fx-background-color: #e81123;
 * }
 * .max-button:ht-max {
 *     -fx-background-color: derive(-fx-base, 20%);
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Hit-spot snapshots are rebuilt on the JavaFX Application Thread but can be read
 * from any thread (native message handler thread). The snapshot pattern ensures
 * readers always see a consistent, immutable list.
 * </p>
 *
 * @author XDSSWAR
 * @version 1.0.5
 * @since 1.0.0
 * @see AbstractNfxUndecoratedWindow
 * @see HitSpot
 */
public abstract class NfxStage extends AbstractNfxUndecoratedWindow {

    /*
     * =================================================================================================================
     *
     *                                    Client Area Hit Regions
     *
     * These regions define areas within the title bar that should receive client (HTCLIENT) hit-test
     * results instead of caption (HTCAPTION). This allows interactive controls in the title bar area.
     * =================================================================================================================
     */

    /**
     * List of client-area regions participating in custom hit testing.
     * <p>
     * Each Region in this list will receive {@code HTCLIENT} in hit-testing, allowing it to
     * receive mouse events normally instead of triggering window drag. Regions are stored
     * in a {@link CopyOnWriteArrayList} for thread-safe iteration during hit-testing while
     * allowing modifications on the FX thread.
     * </p>
     * <p>
     * Coordinates are in DIP (device-independent pixels / logical pixels).
     * </p>
     */
    private final List<Region> hitRegions = new CopyOnWriteArrayList<>();

    /*
     * =================================================================================================================
     *
     *                                    Window Control HitSpots
     *
     * Cached HitSpot instances for the standard window controls. These are created once when
     * the control is assigned and reused until the control is replaced.
     * =================================================================================================================
     */

    /** Cached HitSpot for the close button. Null if no close control is assigned. */
    private HitSpot clHt;

    /** Cached HitSpot for the maximize/restore button. Null if no maximize control is assigned. */
    private HitSpot mxHt;

    /** Cached HitSpot for the minimize button. Null if no minimize control is assigned. */
    private HitSpot miHt;

    /*
     * =================================================================================================================
     *
     *                                    Window Control References
     *
     * References to the JavaFX controls used for window actions. These are kept to manage
     * event handlers and CSS pseudo-classes.
     * =================================================================================================================
     */

    /** The control acting as the close button. Null if not assigned. */
    private Control closeCtrl;

    /** The control acting as the maximize/restore button. Null if not assigned. */
    private Control maxCtrl;

    /** The control acting as the minimize button. Null if not assigned. */
    private Control minCtrl;

    /*
     * =================================================================================================================
     *
     *                                    Snapshot Management
     *
     * The snapshot pattern provides O(1) read access for the native hit-testing hot path while
     * allowing lazy rebuilds only when the UI changes.
     * =================================================================================================================
     */

    /**
     * Dirty flag indicating the HitSpot snapshot needs to be rebuilt.
     * <p>
     * When {@code true}, the next call to {@link #getHitSpots()} will trigger a rebuild of
     * {@link #hitSpotsSnapshot}. This flag is set whenever regions or controls change.
     * </p>
     * <p>
     * Marked volatile for visibility across threads, but writes are synchronized via
     * {@link #snapshotLock} for atomicity with snapshot updates.
     * </p>
     */
    private volatile boolean spotsDirty = true;

    /**
     * Immutable snapshot of all active HitSpots for hit-testing.
     * <p>
     * This list is returned by {@link #getHitSpots()} and is guaranteed to be immutable and
     * non-null. The native layer can safely iterate this list without synchronization.
     * </p>
     * <p>
     * Marked volatile to ensure visibility of reference updates across threads.
     * </p>
     */
    private volatile List<HitSpot> hitSpotsSnapshot = List.of();

    /**
     * Cache mapping each Region to its corresponding HitSpot instance.
     * <p>
     * <strong>FIX: Issue #25</strong> - Changed from IdentityHashMap to ConcurrentHashMap for
     * thread safety. The {@code computeIfAbsent()} method is atomic on ConcurrentHashMap,
     * ensuring each Region gets exactly one HitSpot even under concurrent access.
     * </p>
     */
    private final Map<Region, HitSpot> regionSpotCache = new ConcurrentHashMap<>();

    /**
     * Lock object for synchronizing snapshot rebuilds.
     * <p>
     * <strong>FIX: Issue #11</strong> - Ensures atomicity between checking {@code spotsDirty}
     * and updating the snapshot. Without this lock, a race condition could occur where another
     * thread sets {@code spotsDirty=true} between our check and clear.
     * </p>
     */
    private final Object snapshotLock = new Object();

    /*
     * =================================================================================================================
     *
     *                                    Event Handlers and Listeners
     *
     * Stored references to handlers/listeners so they can be properly removed when controls change.
     * =================================================================================================================
     */

    /** Click handler for the close button. Triggers window close on primary click. */
    private EventHandler<MouseEvent> closeHandler;

    /** Click handler for the maximize button. Toggles between maximized and normal states. */
    private EventHandler<MouseEvent> maxHandler;

    /** Click handler for the minimize button. Iconifies the window. */
    private EventHandler<MouseEvent> minHandler;

    /**
     * Hover listener for the close button's HitSpot.
     * <p>
     * <strong>FIX: Issue #2</strong> - Stored for proper cleanup when the close control changes.
     * Previously, listeners were added but never removed, causing memory leaks and stale
     * pseudo-class updates.
     * </p>
     */
    private ChangeListener<Boolean> closeHoverListener;

    /** Hover listener for the maximize button's HitSpot. */
    private ChangeListener<Boolean> maxHoverListener;

    /** Hover listener for the minimize button's HitSpot. */
    private ChangeListener<Boolean> minHoverListener;

    /*
     * =================================================================================================================
     *
     *                                         Constructors
     *
     * =================================================================================================================
     */

    /**
     * Creates a new undecorated stage that appears in the Windows taskbar.
     * <p>
     * This is the recommended constructor for most applications. The window will
     * appear in the taskbar and can be switched to using Alt+Tab.
     * </p>
     */
    public NfxStage() {
        this(false);
    }

    /**
     * Creates a new undecorated stage with optional taskbar visibility.
     * <p>
     * Windows hidden from the taskbar are useful for auxiliary windows, tool palettes,
     * or popup-style windows that shouldn't appear as separate application entries.
     * </p>
     *
     * @param hideFromTaskBar {@code true} to hide the window from the taskbar,
     *                        {@code false} to show it normally
     */
    public NfxStage(boolean hideFromTaskBar) {
        super(hideFromTaskBar);
    }

    /**
     * Adds one or more client-area regions to participate in custom hit testing.
     * <p>
     * Each {@code Region} is tracked by identity; duplicates are ignored and the
     * insertion order is preserved. Call from the JavaFX Application Thread.
     * Coordinates are in DIP (logical pixels).
     *
     * @param areas one or more regions to include in hit testing; nulls are ignored
     */
    /**
     * FIX: Issue #26 - Synchronize spotsDirty writes to prevent race conditions.
     * All writes to spotsDirty must happen inside synchronized(snapshotLock).
     */
    protected void addClientAreas(Region... areas){
        for (Region area : areas) {
            if (!hitRegions.contains(area)) {
                hitRegions.add(area);
                spotFor(area);
                synchronized (snapshotLock) {
                    spotsDirty = true;
                }
            }
        }
    }

    /**
     * Removes one or more client-area regions from custom hit testing.
     * <p>
     * Matching is by identity. If a region is present, its cached {@code HitSpot}
     * is also discarded. Call from the JavaFX Application Thread.
     *
     * @param areas regions to remove; nulls are ignored
     */
    /**
     * FIX: Issue #26 - Synchronize spotsDirty writes to prevent race conditions.
     */
    protected void removeClientAreas(Region... areas){
        for (Region area : areas) {
            if (hitRegions.remove(area)) {
                area.pseudoClassStateChanged(HT_CLIENT_CLASS, false);
                regionSpotCache.remove(area);
                synchronized (snapshotLock) {
                    spotsDirty = true;
                }
            }
        }
    }

    /**
     * Assigns the control that acts as the Close button in the custom title bar.
     * <p>
     * Replaces any previous control, removing its click handler and clearing the
     * close pseudo-class. Attaches a primary-click handler to invoke {@link #close()}
     * and creates (or replaces) the cached close {@code HitSpot}, which toggles the
     * {@code :ht-close} pseudo-class based on hover.
     *
     * FIX: Issue #2 - Now properly removes hover listener from old HitSpot before creating new one.
     *
     * @param control the control to use as Close; null clears the assignment
     */
    protected void setCloseControl(Control control){
        if (closeCtrl == control) return;

        // FIX: Issue #2 - Remove hover listener from old HitSpot
        if (clHt != null && closeHoverListener != null) {
            clHt.hoveredProperty().removeListener(closeHoverListener);
            closeHoverListener = null;
        }

        // Detach old click handler
        if (closeCtrl != null && closeHandler != null) {
            closeCtrl.removeEventHandler(MouseEvent.MOUSE_CLICKED, closeHandler);
            closeCtrl.pseudoClassStateChanged(HT_CLOSE_CLASS, false);
        }
        closeCtrl = control;

        if (control == null) {
            clHt = null;
            synchronized (snapshotLock) { spotsDirty = true; }
            return;
        }

        closeHandler = e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                //This is necessary to forward the close request event since its not fired automatically in current config
                this.fireEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSE_REQUEST));
                close();
            }
        };
        control.addEventHandler(MouseEvent.MOUSE_CLICKED, closeHandler);

        // Build once & hook pseudo toggle once
        clHt = HitSpot.builder().close(true).window(this).control(control).build();

        // FIX: Issue #2 - Store listener reference so it can be removed later
        closeHoverListener = (obs, o, h) -> control.pseudoClassStateChanged(HT_CLOSE_CLASS, h);
        clHt.hoveredProperty().addListener(closeHoverListener);

        // FIX: Issue #26 - Synchronize spotsDirty write
        synchronized (snapshotLock) { spotsDirty = true; }
    }

    /**
     * Assigns the control that acts as the Maximize/Restore button in the custom title bar.
     * <p>
     * Replaces any previous control, removing its click handler and clearing the
     * maximize pseudo-class. Attaches a primary-click handler that toggles the window
     * state between {@code MAXIMIZED} and {@code NORMAL}. Creates (or replaces) the
     * cached maximize {@code HitSpot}, which updates the {@code :ht-max} pseudo-class
     * based on hover.
     *
     * FIX: Issue #2 - Now properly removes hover listener from old HitSpot before creating new one.
     *
     * @param control the control to use as Maximize/Restore; null clears the assignment
     */
    protected void setMaxControl(Control control){
        if (maxCtrl == control) return;

        // FIX: Issue #2 - Remove hover listener from old HitSpot
        if (mxHt != null && maxHoverListener != null) {
            mxHt.hoveredProperty().removeListener(maxHoverListener);
            maxHoverListener = null;
        }

        // Detach old click handler
        if (maxCtrl != null && maxHandler != null) {
            maxCtrl.removeEventHandler(MouseEvent.MOUSE_CLICKED, maxHandler);
            maxCtrl.pseudoClassStateChanged(HT_MAX_CLASS, false);
        }
        maxCtrl = control;

        if (control == null) {
            mxHt = null;
            synchronized (snapshotLock) { spotsDirty = true; }
            return;
        }

        maxHandler = e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                setWindowState(getWindowState() == WindowState.MAXIMIZED
                        ? WindowState.NORMAL : WindowState.MAXIMIZED);
            }
        };
        control.addEventHandler(MouseEvent.MOUSE_CLICKED, maxHandler);

        mxHt = HitSpot.builder().maximize(true).window(this).control(control).build();

        // FIX: Issue #2 - Store listener reference so it can be removed later
        maxHoverListener = (obs, o, h) -> control.pseudoClassStateChanged(HT_MAX_CLASS, h);
        mxHt.hoveredProperty().addListener(maxHoverListener);

        // FIX: Issue #26 - Synchronize spotsDirty write
        synchronized (snapshotLock) { spotsDirty = true; }
    }

    /**
     * Assigns the control that acts as the Minimize button in the custom title bar.
     * <p>
     * Replaces any previous control, removing its click handler and clearing the
     * minimize pseudo-class. Attaches a primary-click handler that iconifies the
     * window via {@code setIconified(true)}. Creates (or replaces) the cached
     * minimize {@code HitSpot}, which toggles the {@code :ht-min} pseudo-class
     * based on hover.
     *
     * FIX: Issue #2 - Now properly removes hover listener from old HitSpot before creating new one.
     *
     * @param control the control to use as Minimize; null clears the assignment
     */
    protected void setMinControl(Control control){
        if (minCtrl == control) return;

        // FIX: Issue #2 - Remove hover listener from old HitSpot
        if (miHt != null && minHoverListener != null) {
            miHt.hoveredProperty().removeListener(minHoverListener);
            minHoverListener = null;
        }

        // Detach old click handler
        if (minCtrl != null && minHandler != null) {
            minCtrl.removeEventHandler(MouseEvent.MOUSE_CLICKED, minHandler);
            minCtrl.pseudoClassStateChanged(HT_MIN_CLASS, false);
        }
        minCtrl = control;

        if (control == null) {
            miHt = null;
            synchronized (snapshotLock) { spotsDirty = true; }
            return;
        }

        minHandler = e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                setIconified(true);
            }
        };
        control.addEventHandler(MouseEvent.MOUSE_CLICKED, minHandler);

        miHt = HitSpot.builder().minimize(true).window(this).control(control).build();

        // FIX: Issue #2 - Store listener reference so it can be removed later
        minHoverListener = (obs, o, h) -> control.pseudoClassStateChanged(HT_MIN_CLASS, h);
        miHt.hoveredProperty().addListener(minHoverListener);

        // FIX: Issue #26 - Synchronize spotsDirty write
        synchronized (snapshotLock) { spotsDirty = true; }
    }

    /**
     * Returns the current immutable snapshot of all HitSpots used for hit testing.
     * The snapshot is rebuilt lazily only when inputs change, making this call O(1)
     * on the hot path. Do not mutate the returned list.
     *
     * @return immutable snapshot of HitSpots in hit-test order
     */
    @Override
    protected final List<HitSpot> getHitSpots() {
        rebuildHitSpotsSnapshotIfNeeded();
        return hitSpotsSnapshot;
    }


    /**
     * Rebuilds the immutable HitSpot snapshot if marked dirty.
     * Collects cached region spots (preserving hitRegions order) and the
     * close/maximize/minimize spots, then publishes a new unmodifiable list
     * to hitSpotsSnapshot and clears the dirty flag.
     * Intended to run on the FX thread; the volatile snapshot supports
     * lock-free reads on the hot path.
     *
     * FIX: Issue #11 - Use synchronized block to ensure atomicity between
     * checking spotsDirty and updating the snapshot. This prevents a race
     * where another thread sets spotsDirty=true between our check and clear.
     */
    private void rebuildHitSpotsSnapshotIfNeeded() {
        // FIX: Issue #11 - Synchronize to prevent race condition
        synchronized (snapshotLock) {
            if (!spotsDirty) return;

            var list = new ArrayList<HitSpot>(hitRegions.size() + 3);
            for (Region r : hitRegions) {
                list.add(spotFor(r));
            }
            if (clHt != null) list.add(clHt);
            if (mxHt != null) list.add(mxHt);
            if (miHt != null) list.add(miHt);

            hitSpotsSnapshot = List.copyOf(list);
            spotsDirty = false;
        }
    }

    /**
     * Returns the cached {@link HitSpot} for the given {@link Region}, creating and caching
     * it on first use. On creation, a listener is attached that toggles the {@code :ht-client}
     * pseudo-class on the region while the spot is hovered. The cache is identity-based
     * (one {@code HitSpot} per {@code Region}).
     * <p>
     * Call on the JavaFX Application Thread.
     *
     * @param r the region to associate with a {@code HitSpot} (non-null)
     * @return the cached or newly created {@code HitSpot} for {@code r}
     */

    private HitSpot spotFor(Region r) {
        return regionSpotCache.computeIfAbsent(r, rr -> {
            HitSpot ht = HitSpot.builder()
                    .window(this)
                    .control(rr)   // Region is a Node → supports pseudoClassStateChanged
                    .build();

            // When this spot is hovered, flip the :ht-client pseudo on the Region
            ht.hoveredProperty().addListener((obs, o, h) ->
                    rr.pseudoClassStateChanged(HT_CLIENT_CLASS, h));
            return ht;
        });
    }

}
