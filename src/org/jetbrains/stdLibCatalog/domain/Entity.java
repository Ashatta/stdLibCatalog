package org.jetbrains.stdLibCatalog.domain;

import java.net.URL;

public abstract class Entity {
    private final String name;
    private final Language lang;
    private final String documentation;
    private final URL docLink;
    private PackageEntity containingPackage;

    public Entity(String name, Language lang, String documentation, URL docLink) {
        this.name = name;
        this.lang = lang;
        this.documentation = documentation;
        this.docLink = docLink;
        this.containingPackage = null;
    }

    public String getName() {
        return name;
    }

    public Language getLang() {
        return lang;
    }

    public String getDocumentation() {
        return documentation;
    }

    public URL getDocLink() {
        return docLink;
    }

    public PackageEntity getContainingPackage() {
        return containingPackage;
    }

    public void setContainingPackage(PackageEntity containingPackage) {
        this.containingPackage = containingPackage;
    }

    public String toString() {
        return "Name: " + name
                + "\nLanguage: " + lang.name()
                + "\nDoc: " + (docLink != null ? docLink.toString() : "null")
                + "\nContaining package: " + (containingPackage != null ? containingPackage.getName() : "null");
    }
}
