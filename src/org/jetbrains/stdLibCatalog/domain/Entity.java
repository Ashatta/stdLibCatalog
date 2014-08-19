package org.jetbrains.stdLibCatalog.domain;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class Entity {
    private final String name;
    private final Language lang;
    private final String documentation;
    private final URL docLink;
    private PackageEntity containingPackage;
    private final Map<String, String> attributes = new HashMap<>();

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
        String result = "Name: " + name
                + "\nLanguage: " + lang.name()
                + "\nDoc: " + (docLink != null ? docLink.toString() : "null")
                + "\nContaining package: " + (containingPackage != null ? containingPackage.getName() : "null");

        result += "\nattributes {";
        for (Map.Entry<String, String> attr : attributes.entrySet()) {
            result += "\n\t" + attr.toString();
        }
        result += "\n}";

        return result;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getAttr(String key) {
        return attributes.get(key);
    }

    public void setAttr(String key, String value) {
        attributes.put(key, value);
    }

    public boolean hasAttr(String key) {
        return attributes.containsKey(key);
    }
}
