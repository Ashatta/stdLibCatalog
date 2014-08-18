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

    public String toString() {
        String result = "<function> {\n[parameters]";

        for (Type param : parameters) {
            result += "\n" + param.toString();
        }

        result += "\n[result]\n" + result.toString() + "}";
        return result;
    }
}
