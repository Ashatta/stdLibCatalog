package org.jetbrains.stdLibCatalog.domain;

import org.jsoup.helper.StringUtil;

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

    public String toString() {
        String result = getName() + ": ";
        for (Constraint constraint : constraints) {
            result += constraint.toString() + ", ";
        }
        return result.substring(0, result.length() - 2);
    }
}
