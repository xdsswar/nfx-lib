package xss.it.nfx;

import com.sun.it.nfx.Rect;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.Region;

/**
 * @author XDSSWAR
 * Created on 09/29/2023
 */
public final class HitSpot {
    /**
     * The Node control for the HitSpot.
     */
    private final Region control;

    /**
     * Flag indicating whether the HitSpot should have a close functionality.
     */
    private final boolean close;

    /**
     * Flag indicating whether the HitSpot should have a maximize functionality.
     */
    private final boolean maximize;

    /**
     * Flag indicating whether the HitSpot should have a minimize functionality.
     */
    private final boolean minimize;

    /**
     * Flag indicating whether the HitSpot should have a system menu functionality.
     */
    private final boolean systemMenu;

    /**
     * Flag indicating whether the HitSpot is a client.
     */
    private final boolean client;

    /**
     * Hover property
     */
    private BooleanProperty hovered;

    /**
     * Constructs a HitSpot object using a builder pattern to specify interaction options.
     *
     * @param builder The builder containing interaction options.
     */
    private HitSpot(Builder builder) {
        this.control = builder.control;
        this.close = builder.close;
        this.maximize = builder.maximize;
        this.minimize = builder.minimize;
        this.systemMenu = builder.systemMenu;
        this.client = !close && !maximize && !minimize && !systemMenu;
    }


    /**
     * Returns the BooleanProperty representing the hover status.
     * If not already initialized, it creates a new BooleanProperty with a default value of false.
     *
     * @return The BooleanProperty for hover status.
     */
    public BooleanProperty hoveredProperty() {
        if (hovered == null) {
            hovered = new SimpleBooleanProperty(this, "hovered", false);
        }
        return hovered;
    }

    /**
     * Checks if the object is currently being hovered over.
     *
     * @return True if the object is being hovered over, false otherwise.
     */
    public boolean isHovered() {
        return hoveredProperty().get();
    }

    /**
     * Sets the hover status of the object.
     *
     * @param hovered True to indicate that the object is being hovered over, false otherwise.
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
     *
     * @return The Rectangle2D representing the bounds of the HitSpot.
     */
    public Rectangle2D getRect(){
        return Rect.createFromBounds(control.localToScene(control.getBoundsInLocal()));
    }

    /**
     * Builder
     */
    public final static class Builder {
        /**
         * The Node control for the HitSpot.
         */
        private Region control;

        /**
         * Flag indicating whether the HitSpot should have a close functionality.
         */
        private boolean close = false;

        /**
         * Flag indicating whether the HitSpot should have a maximize functionality.
         */
        private boolean maximize = false;

        /**
         * Flag indicating whether the HitSpot should have a minimize functionality.
         */
        private boolean minimize = false;

        /**
         * Flag indicating whether the HitSpot should have a system menu functionality.
         */
        private boolean systemMenu = false;


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
         * Builds and returns the HitSpot object using the Builder's configuration.
         *
         * @return the constructed HitSpot object
         */
        public HitSpot build() {
            return new HitSpot(this);
        }

    }


    /**
     * Returns a new instance of the Builder class to start building a HitSpot object.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

}
