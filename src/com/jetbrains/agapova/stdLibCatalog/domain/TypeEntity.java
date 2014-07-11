package com.jetbrains.agapova.stdLibCatalog.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ashatta on 7/10/14.
 */
public class TypeEntity {
    private String id;
    private String name;
    private String lang;
    private String documentation;
    private List<FunctionEntity> functions;
    private List<TypeEntity> subTypes;
    private List<TypeEntity> superTypes;
    private PackageEntity containingPackage;
    private List<TypeLink> links;
    private String docLink;

    public TypeEntity(String id, String name, String lang, String documentation, List<FunctionEntity> functions
            , List<TypeEntity> subTypes, List<TypeEntity> superTypes, PackageEntity containingPackage, String docLink) {
        this.id = id;
        this.name = name;
        this.lang = lang;
        this.documentation = documentation;
        this.functions = functions;
        this.subTypes = subTypes;
        this.superTypes = superTypes;
        this.containingPackage = containingPackage;
        this.links = new ArrayList<TypeLink>();
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

    public List<FunctionEntity> getFunctions() {
        return functions;
    }

    public List<TypeEntity> getSubTypes() {
        return subTypes;
    }

    public List<TypeEntity> getSuperTypes() {
        return superTypes;
    }

    public PackageEntity getContainingPackage() {
        return containingPackage;
    }

    public List<TypeLink> getLinks() {
        return links;
    }

    public String getDocLink() {
        return docLink;
    }
}
