package com.jetbrains.agapova.stdLibCatalog.domain;

import java.util.List;

/**
 * Created by ashatta on 7/10/14.
 */
public class Signature {
    private List<TypeEntity> arguments;
    private TypeEntity result;

    public Signature(List<TypeEntity> arguments, TypeEntity result) {
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
