package org.jetbrains.stdLibCatalog.domain;

import java.util.List;

public class DataType extends TypeEntity {
    private final TypeConstructor typeConstructor;
    private final List<TypeEntity> parameters;

    public DataType(TypeConstructor typeConstructor, List<TypeEntity> parameters) {
        this.typeConstructor = typeConstructor;
        this.parameters = parameters;
    }


    //todo dphJB not used!
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    //todo dphJB not used!
    public List<TypeEntity> getParameters() {
        return parameters;
    }
}
