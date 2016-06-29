package com.github.wuic.nut;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Default implementation of {@link Source}.
 * </p>
 *
 * @author Guillaume DROUET
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
        return new ArrayList<ConvertibleNut>(sources);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addOriginalNut(final ConvertibleNut convertibleNut) {
        sources.add(convertibleNut);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replaceOriginalNut(final ConvertibleNut original, final ConvertibleNut replacement) {
        if (sources.remove(original)) {
            sources.add(replacement);
            return true;
        }

        return false;
    }
}
