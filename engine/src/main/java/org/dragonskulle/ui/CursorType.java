/* (C) 2021 DragonSkulle */
package org.dragonskulle.ui;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.ResourceManager;

/** CursorType allows us to declare different images and xhot/yhot offsets for the image. */
@Accessors(prefix = "m")
public enum CursorType {
    DEFAULT("ui/cursor.png", 10, 10),
    HOVER("ui/cursor_hover.png", 16, 8),
    INCLICK("ui/cursor_inclick.png", 20, 9);

    @Getter private final String mPath;
    @Getter private final int mXHot;
    @Getter private final int mYHot;

    /**
     * Constructor.
     *
     * @param path the path to be resolved in {@link ResourceManager}
     * @param xhot the x offset to the point of the image
     * @param yhot the y offset to the point of the image
     */
    CursorType(String path, int xhot, int yhot) {
        mPath = path;
        mXHot = xhot;
        mYHot = yhot;
    }
}
