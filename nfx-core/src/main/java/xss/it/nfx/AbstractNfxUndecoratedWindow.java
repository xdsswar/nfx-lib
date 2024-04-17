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

import java.util.List;

/**
 * @author XDSSWAR
 * Created on 04/16/2024
 */
public abstract class AbstractNfxUndecoratedWindow extends NfxWindow {


    public AbstractNfxUndecoratedWindow() {
        super();
        initialize();
    }


    private void initialize(){

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

}
