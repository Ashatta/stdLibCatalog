package com.jetbrains.agapova.stdLibCatalog.domain;

/**
 * Created by ashatta on 7/12/14.
 */
public class Entity {
    protected String id;
    protected String name;
    protected String lang;
    protected String documentation;
    protected String docLink;

    public Entity(String id, String name, String lang, String documentation, String docLink) {
        this.id = id;
        this.name = name;
        this.lang = lang;
        this.documentation = documentation;
        this.docLink = docLink;
    }

    public String getId() {
        return id;
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
}
