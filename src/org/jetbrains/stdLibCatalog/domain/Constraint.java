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

    public String getDeclaration() {
        return declaration;
    }

    public String toString() {
        String result = "(" + declaration + "; ";

        for (TypeVariable variable : variables) {
            result += variable.getName() + ", ";
        }
        result = result.substring(0, result.length() - 2) + "; ";

        for (Entity entity : otherEntities) {
            PackageEntity containingPackage = entity.getContainingPackage();
            result += (containingPackage != null ? containingPackage.getName() : "null") + "::" + entity.getName() + ", ";
        }
        result = result.substring(0, result.length() - 2) + ")";

        return result;
    }
}
