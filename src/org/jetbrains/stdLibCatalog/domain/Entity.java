package org.jetbrains.stdLibCatalog.domain;

public class Entity {
    private final String name;
    private final String lang;
    private final String documentation;
    private final String docLink;
    private PackageEntity containingPackage;

    public Entity(String name, String lang, String documentation, String docLink) {
        this.name = name;
        this.lang = lang;
        this.documentation = documentation;
        this.docLink = docLink;
        this.containingPackage = null;
    }

    public String getName() {
        return name;
    }

    public String getLang() {
        return lang;
    }

    public String getDocumentation() {
        return documentation;
    }

    public String getDocLink() {
        return docLink;
    }

    public PackageEntity getContainingPackage() {
        return containingPackage;
    }

    public void setContainingPackage(PackageEntity containingPackage) {
        this.containingPackage = containingPackage;
    }
}
