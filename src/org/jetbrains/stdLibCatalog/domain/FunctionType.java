package org.jetbrains.stdLibCatalog.domain;

import java.util.List;

public class FunctionType extends TypeEntity {
    private final List<TypeEntity> arguments;
    private final TypeEntity result;

    public FunctionType(List<TypeEntity> arguments, TypeEntity result) {
        this.arguments = arguments;
        this.result = result;
    }

    public List<TypeEntity> getArguments() {
        return arguments;
    }

    public TypeEntity getResult() {
        return result;
    }
}
