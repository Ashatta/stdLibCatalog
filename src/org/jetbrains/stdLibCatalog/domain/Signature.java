package org.jetbrains.stdLibCatalog.domain;

import java.util.List;

public class Signature {
    // functions can take other functions as arguments, so I will need to change things here a bit
    // maybe make a base class for functions and types derived from Entity
    private List<TypedEntity> arguments;
    private TypedEntity result;

    public Signature(List<TypedEntity> arguments, TypedEntity result) {
        this.arguments = arguments;
        this.result = result;
    }

    public List<TypedEntity> getArguments() {
        return arguments;
    }

    public TypedEntity getResult() {
        return result;
    }
}
