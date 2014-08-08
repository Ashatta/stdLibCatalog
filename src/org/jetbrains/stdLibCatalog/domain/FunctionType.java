package org.jetbrains.stdLibCatalog.domain;

import java.util.List;

public class FunctionType extends Type {
    private final List<Type> parameters;
    private final Type result;

    public FunctionType(List<Type> parameters, Type result) {
        this.parameters = parameters;
        this.result = result;
    }

    public List<Type> getParameters() {
        return parameters;
    }

    public Type getResult() {
        return result;
    }
}
