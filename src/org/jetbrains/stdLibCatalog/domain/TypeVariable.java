package org.jetbrains.stdLibCatalog.domain;

import java.util.ArrayList;
import java.util.List;

public class TypeVariable extends TypeConstructor {
    private final List<Constraint> constraints = new ArrayList<>();

    public TypeVariable(String name, Language language) {
        super(name, language, "", null);
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public void addConstraint(Constraint constraint) {
        constraints.add(constraint);
    }
}
