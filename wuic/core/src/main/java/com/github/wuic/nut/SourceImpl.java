package com.github.wuic.nut;

import java.io.Serializable;
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
public class SourceImpl implements Source, Serializable {

    /**
     * The original nuts.
     */
    private final List<ConvertibleNut> sources;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public SourceImpl() {
        sources = new ArrayList<ConvertibleNut>();
    }

    /**
     * <p>
     * Builds a new instance by copy.
     * </p>
     *
     * @param source the source to copy
     */
    public SourceImpl(final Source source) {
        sources = new ArrayList<ConvertibleNut>(source.getOriginalNuts());
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
}
