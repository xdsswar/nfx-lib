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

import javafx.event.EventHandler;
import javafx.scene.control.Control;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * nfx
 * <p>
 * Description:
 * This class is part of the xss.it.nfx package.
 *
 * <p>
 *
 * @author XDSSWAR
 * @version 1.0
 * @since August 13, 2025
 * <p>
 * Created on 08/13/2025 at 17:57
 * Base class for an undecorated window with custom title-bar hit testing.
 * Caches HitSpots and exposes a fast snapshot for the native HT logic.
 */
public abstract class NfxStage extends AbstractNfxUndecoratedWindow {
    /**
     *  Client-area regions (in DIP) that participate in hit testing; updated on the FX thread.
     */
    private final List<Region> hitRegions = new CopyOnWriteArrayList<>();

    /**
     * Cached HitSpots for window controls: close, maximize/restore, and minimize (built once, reused).
     */
    private HitSpot clHt, mxHt, miHt;

    /**
     * Assigned JavaFX controls for the window actions; used to attach/remove handlers and CSS pseudos.
     */
    private Control closeCtrl, maxCtrl, minCtrl;

    /**
     * Dirty flag: when true, the HitSpot snapshot must be rebuilt before being served.
     */
    private volatile boolean spotsDirty = true;

    /**
     * Immutable snapshot of the current HitSpots; returned by getHitSpots() for O(1) reads.
     */
    private volatile List<HitSpot> hitSpotsSnapshot = List.of();

    /**
     * Cache mapping each Region to its single HitSpot instance (identity-based to avoid listener churn).
     */
    private final Map<Region, HitSpot> regionSpotCache = new IdentityHashMap<>();

    /**
     * Mouse click handlers for the window controls; kept so they can be removed on reassignment.
     */
    private EventHandler<MouseEvent> closeHandler, maxHandler, minHandler;

    /**
     * Creates a new undecorated stage shown in the taskbar by default.
     */
    public NfxStage() {
        this(false);
    }

    /**
     * Creates a new undecorated stage.
     *
     * @param hideFromTaskBar if true, the window is hidden from the taskbar.
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
    protected void addClientAreas(Region... areas){
        for (Region area : areas) {
            if (!hitRegions.contains(area)) {
                hitRegions.add(area);
                spotFor(area);
                spotsDirty = true;
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
    protected void removeClientAreas(Region... areas){
        for (Region area : areas) {
            if (hitRegions.remove(area)) {
                area.pseudoClassStateChanged(HT_CLIENT_CLASS, false);
                regionSpotCache.remove(area);
                spotsDirty = true;
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
     * @param control the control to use as Close; null clears the assignment
     */
    protected void setCloseControl(Control control){
        if (closeCtrl == control) return;

        // Detach old
        if (closeCtrl != null && closeHandler != null) {
            closeCtrl.removeEventHandler(MouseEvent.MOUSE_CLICKED, closeHandler);
            closeCtrl.pseudoClassStateChanged(HT_CLOSE_CLASS, false);
        }
        closeCtrl = control;

        if (control == null) { clHt = null; spotsDirty = true; return; }

        closeHandler = e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                close();
            }
        };
        control.addEventHandler(MouseEvent.MOUSE_CLICKED, closeHandler);

        // Build once & hook pseudo toggle once
        clHt = HitSpot.builder().close(true).window(this).control(control).build();
        clHt.hoveredProperty().addListener((obs, o, h) ->
                control.pseudoClassStateChanged(HT_CLOSE_CLASS, h)
        );
        spotsDirty = true;
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
     * @param control the control to use as Maximize/Restore; null clears the assignment
     */
    protected void setMaxControl(Control control){
        if (maxCtrl == control) return;

        if (maxCtrl != null && maxHandler != null) {
            maxCtrl.removeEventHandler(MouseEvent.MOUSE_CLICKED, maxHandler);
            maxCtrl.pseudoClassStateChanged(HT_MAX_CLASS, false);
        }
        maxCtrl = control;

        if (control == null) { mxHt = null; spotsDirty = true; return; }

        maxHandler = e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                setWindowState(getWindowState() == WindowState.MAXIMIZED
                        ? WindowState.NORMAL : WindowState.MAXIMIZED);
            }
        };
        control.addEventHandler(MouseEvent.MOUSE_CLICKED, maxHandler);

        mxHt = HitSpot.builder().maximize(true).window(this).control(control).build();
        mxHt.hoveredProperty().addListener((obs, o, h) ->
                control.pseudoClassStateChanged(HT_MAX_CLASS, h)
        );
        spotsDirty = true;
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
     * @param control the control to use as Minimize; null clears the assignment
     */
    protected void setMinControl(Control control){
        if (minCtrl == control) return;

        if (minCtrl != null && minHandler != null) {
            minCtrl.removeEventHandler(MouseEvent.MOUSE_CLICKED, minHandler);
            minCtrl.pseudoClassStateChanged(HT_MIN_CLASS, false);
        }
        minCtrl = control;

        if (control == null) { miHt = null; spotsDirty = true; return; }

        minHandler = e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                setIconified(true);
            }
        };
        control.addEventHandler(MouseEvent.MOUSE_CLICKED, minHandler);

        miHt = HitSpot.builder().minimize(true).window(this).control(control).build();
        miHt.hoveredProperty().addListener((obs, o, h) ->
                control.pseudoClassStateChanged(HT_MIN_CLASS, h)
        );
        spotsDirty = true;
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
     */
    private void rebuildHitSpotsSnapshotIfNeeded() {
        if (!spotsDirty) return;

        var list = new ArrayList<HitSpot>(hitRegions.size() + 3);
        for (Region r : hitRegions) {
            list.add(spotFor(r));       // <-- and here
        }
        if (clHt != null) list.add(clHt);
        if (mxHt != null) list.add(mxHt);
        if (miHt != null) list.add(miHt);

        hitSpotsSnapshot = List.copyOf(list);
        spotsDirty = false;
    }

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
