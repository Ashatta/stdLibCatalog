package org.jetbrains.stdLibCatalog.domain;

import org.jsoup.helper.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Wildcard extends TypeConstructor {
    private final List<Type> lowerBounds = new ArrayList<>();
    private final List<Type> upperBounds = new ArrayList<>();

    public Wildcard(Language lang) {
        super("?", lang, "", null);
    }

    public void addLowerBound(Type bound) {
        lowerBounds.add(bound);
    }

    public void addUpperBound(Type bound) {
        upperBounds.add(bound);
    }

    public String toString() {
        if (lowerBounds.isEmpty() && upperBounds.isEmpty()) {
            return "?";
        }

        String result = "? {";
        for (Type bound : lowerBounds) {
            result += "\n\tsuper " + StringUtil.join(Arrays.asList(bound.toString().split("\n")), "\n\t");
        }
        for (Type bound : upperBounds) {
            result += "\n\textends " + StringUtil.join(Arrays.asList(bound.toString().split("\n")), "\n\t");
        }
        result += "\n}";

        return result;
    }
}
