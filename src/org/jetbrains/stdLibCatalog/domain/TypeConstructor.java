package org.jetbrains.stdLibCatalog.domain;

import java.net.URL;

public abstract class TypeConstructor extends Entity {
    public TypeConstructor(String name, Language lang, String documentation, URL docLink) {
        super(name, lang, documentation, docLink);
    }
}
