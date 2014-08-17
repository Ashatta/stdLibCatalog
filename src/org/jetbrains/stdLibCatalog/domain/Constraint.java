package org.jetbrains.stdLibCatalog.domain;

import java.util.ArrayList;
import java.util.List;

public class Constraint {
    private final List<TypeVariable> variables;
    private final List<Entity> otherEntities;
    private final String declaration;

    public Constraint(List<TypeVariable> variables, List<Entity> otherEntities, String declaration) {
        this.variables = variables;
        this.otherEntities = otherEntities;
        this.declaration = declaration;
    }

    public List<TypeVariable> getVariables() {
        return variables;
    }
}
