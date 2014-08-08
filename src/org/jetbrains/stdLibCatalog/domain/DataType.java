package org.jetbrains.stdLibCatalog.domain;

import java.util.List;

public class DataType extends Type {
    private final TypeConstructor typeConstructor;
    private final List<Type> arguments;

    public DataType(TypeConstructor typeConstructor, List<Type> arguments) {
        this.typeConstructor = typeConstructor;
        this.arguments = arguments;
    }

    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    public List<Type> getArguments() {
        return arguments;
    }
}
