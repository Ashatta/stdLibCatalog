package org.jetbrains.stdLibCatalog.domain;

public abstract class TypeConstructor extends Entity {
    public TypeConstructor(String name, String lang, String documentation, String docLink) {
        super(name, lang, documentation, docLink);
    }
}
