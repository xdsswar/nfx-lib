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

package com.sun.it.nfx;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

/**
 * @author XDSSWAR
 * Created on 04/17/2024
 */
public final class RestartableTimer {
    /**
     * Private field representing the duration of an action
     */
    private final Duration duration;

    /**
     * Private field representing the event handler for an action
     */
    private final EventHandler<ActionEvent> action;

    /**
     * Private field representing the timeline for controlling animations
     */
    private Timeline timeline;


    /**
     * Creates a RestartableTimer with the specified delay and action.
     *
     * @param delay  the delay in milliseconds
     * @param action the action to be performed on each timer tick
     */
    public RestartableTimer(int delay, EventHandler<ActionEvent> action) {
        this.duration = Duration.millis(delay);
        this.action = action;
        createTimeline();
    }

    /**
     * Creates the Timeline with the specified duration and action.
     */
    private void createTimeline() {
        timeline = new Timeline(new KeyFrame(duration, action));
        timeline.setCycleCount(1);
    }

    /**
     * Starts the timer.
     */
    public void start() {
        timeline.play();
    }

    /**
     * Stops the timer.
     */
    public void stop() {
        timeline.stop();
    }

    /**
     * Restarts the timer.
     */
    public void restart() {
        stop();
        createTimeline();
        start();
    }

}
