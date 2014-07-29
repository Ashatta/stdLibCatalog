package org.jetbrains.stdLibCatalog.domain;

import java.util.List;

public class TypeVariable extends TypeConstructor {
    private final int number;
    private final List<Classifier> constraints;

    public TypeVariable(int number, List<Classifier> constraints) {
        super("", "", "", "");
        this.number = number;
        this.constraints = constraints;
    }

    public List<Classifier> getConstraints() {
        return constraints;
    }
}
