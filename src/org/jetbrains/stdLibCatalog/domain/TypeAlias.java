package org.jetbrains.stdLibCatalog.domain;

import java.util.ArrayList;
import java.util.List;

public class TypeAlias extends TypeConstructor {
    private Type aliasedType;
    private final List<TypeVariable> parameters = new ArrayList<>();
    private final String definition;

    public TypeAlias(String name, String lang, String documentation, String docLink, String definition) {
        super(name, lang, documentation, docLink);
        this.definition = definition;
    }

    public Type getAliasedType() {
        return aliasedType;
    }

    public void setAliasedType(Type aliasedType) {
        this.aliasedType = aliasedType;
    }

    public void addParameter(TypeVariable parameter) {
        parameters.add(parameter);
    }
}
