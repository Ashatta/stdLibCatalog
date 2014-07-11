package com.jetbrains.agapova.stdLibCatalog.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ashatta on 7/10/14.
 */
public class FunctionEntity {
    private String id;
    private String lang;
    private String name;
    private Signature signature;
    private String documentation;
    private TypeEntity containingType;
    private PackageEntity containingPackage;
    private List<FunctionLink> links;
    private String docLink;

    public FunctionEntity(String id, String lang, String name, Signature signature, String documentation
            , TypeEntity containingType, PackageEntity containingPackage, String docLink) {
        this.id = id;
        this.lang = lang;
        this.name = name;
        this.signature = signature;
        this.documentation = documentation;
        this.containingType = containingType;
        this.containingPackage = containingPackage;
        this.links = new ArrayList<FunctionLink>();
        this.docLink = docLink;
    }

    public String getId() {
        return id;
    }

    public String getLang() {
        return lang;
    }

    public String getName() {
        return name;
    }

    public Signature getSignature() {
        return signature;
    }

    public String getDocumentation() {
        return documentation;
    }

    public TypeEntity getContainingType() {
        return containingType;
    }

    public PackageEntity getContainingPackage() {
        return containingPackage;
    }

    public List<FunctionLink> getLinks() {
        return links;
    }

    public String getDocLink() {
        return docLink;
    }
}
