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

package xss.it.demo;

import xss.it.nfx.AbstractNfxUndecoratedWindow;
import xss.it.nfx.HitSpot;

import java.util.List;

/**
 * @author XDSSWAR
 * Created on 04/17/2024
 */
public class Window extends AbstractNfxUndecoratedWindow {

    public Window() {
        super();
    }

    public Window(boolean hideFromTaskBar) {
        super(hideFromTaskBar);
    }

    @Override
    public List<HitSpot> getHitSpots() {
        return List.of();
    }

    @Override
    public double getTitleBarHeight() {
        return 50;
    }
}
