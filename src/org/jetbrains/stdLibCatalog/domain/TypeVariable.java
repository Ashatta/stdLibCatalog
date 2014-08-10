package org.jetbrains.stdLibCatalog.domain;

import java.net.URL;
import java.util.List;

public class TypeVariable extends TypeConstructor {
    private final int index;
    private final List<Classifier> constraints;

    public TypeVariable(String name, Language language, int index, List<Classifier> constraints) {
        super(name, language, "", null);
        this.index = index;
        this.constraints = constraints;
    }

    public List<Classifier> getConstraints() {
        return constraints;
    }
}
