package org.jetbrains.stdLibCatalog.domain;

import java.util.ArrayList;
import java.util.List;

public class TypeAlias extends TypeConstructor {
    private TypeEntity aliasedType;
    private final List<TypeConstructor> parameters = new ArrayList<>();
    private final List<TypeLink> links = new ArrayList<>();
    private final String definition;

    public TypeAlias(String name, String lang, String documentation, String docLink, String definition) {
        super(name, lang, documentation, docLink);
        this.definition = definition;
    }

    public TypeEntity getAliasedType() {
        return aliasedType;
    }

    public void setAliasedType(TypeEntity aliasedType) {
        this.aliasedType = aliasedType;
    }

    public List<TypeLink> getLinks() {
        return links;
    }

    public void addParameter(TypeConstructor parameter) {
        parameters.add(parameter);
    }
}
