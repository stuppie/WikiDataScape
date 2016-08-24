/*
https://github.com/svn2github/cytoscape/blob/a3df8f63dba4ec49942027c91ecac6efa920c195/csplugins/trunk/ucsd/mes/cy3-shared-local-tables/impl/vizmap-gui-impl/src/main/java/org/cytoscape/view/vizmap/gui/internal/util/mapgenerator/RandomColorMappingGenerator.java
 */
package org.cytoscape.WikiDataScape.internal;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.cytoscape.view.vizmap.gui.util.DiscreteMappingGenerator;

/**
 * Mapping generator from any attributes to random color
 */
public class RandomColorMappingGenerator implements DiscreteMappingGenerator<Color> {

    private final int MAX_COLOR = 256 * 256 * 256;
    private final long seed = System.currentTimeMillis();
    private final Random rand = new Random(seed);

    @Override
    public <T> Map<T, Color> generateMap(Set<T> attributeSet) {
        final Map<T, Color> valueMap = new HashMap<>();

        for (T key : attributeSet) {
            valueMap.put(key, new Color(((Number) (rand.nextFloat() * MAX_COLOR)).intValue()));
        }

        return valueMap;
    }

    @Override
    public Class<Color> getDataType() {
        return Color.class;
    }
}
