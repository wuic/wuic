package com.github.wuic.nut;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Default implementation of {@link Source}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.5.3
 */
public class SourceImpl implements Source {

    /**
     * The original nuts.
     */
    private final List<ConvertibleNut> sources;

    /**
     * <p>
     * Builds a new instane.
     * </p>
     */
    public SourceImpl() {
        sources = new ArrayList<ConvertibleNut>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> getOriginalNuts() {
        return sources;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addOriginalNut(final ConvertibleNut convertibleNut) {
        sources.add(convertibleNut);
    }
}
