package org.jetbrains.stdLibCatalog.domain;

import java.util.List;

public class TypeVariable extends TypeConstructor {
    private final int index;
    private final List<Classifier> constraints;

    public TypeVariable(String name, String language, int index, List<Classifier> constraints) {
        super(name, language, "", "");
        this.index = index;
        this.constraints = constraints;
    }

    public List<Classifier> getConstraints() {
        return constraints;
    }
}
