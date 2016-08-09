package org.cytoscape.WikiDataScape.internal;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cytoscape.view.vizmap.gui.util.DiscreteMappingGenerator;

public class RainbowColorMappingGenerator implements DiscreteMappingGenerator<Color> {

    public <T> Map<T, Color> generateMap(Set<T> attributeSet) {
        // Error check
        if (attributeSet == null || attributeSet.isEmpty()) {
            return null;
        }

        final float increment = 1f / ((Number) attributeSet.size()).floatValue();

        float hue = 0;

        final Map<T, Color> valueMap = new HashMap<>();

        for (T key : attributeSet) {
            hue = hue + increment;
            valueMap.put(key, new Color(Color.HSBtoRGB(hue, 1f, 1f)));
        }

        return valueMap;
    }

    @Override
    public Class<Color> getDataType() {
        return Color.class;
    }

}
