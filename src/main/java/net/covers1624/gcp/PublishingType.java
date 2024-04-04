package net.covers1624.gcp;

/**
 * Created by covers1624 on 3/4/24.
 */
public enum PublishingType {
    /**
     * Stops the deployment on VALIDATED awaiting user approval in the panel.
     */
    USER_MANGED,
    /**
     * Automatically deploy after validation.
     */
    AUTOMATIC
}
