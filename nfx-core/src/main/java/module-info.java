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

/**
 * @author XDSSWAR
 * Created on 04/13/2024
 */
module nfx.core {
    requires javafx.base;
    requires javafx.graphics;
    requires java.desktop;
    requires jdk.dynalink;

    exports xss.it.nfx;
    opens xss.it.nfx;
}