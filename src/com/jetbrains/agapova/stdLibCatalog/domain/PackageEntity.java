package com.jetbrains.agapova.stdLibCatalog.domain;

import java.util.List;

/**
 * Created by ashatta on 7/10/14.
 */
public class PackageEntity {
    private String id;
    private String name;
    private String lang;
    private List<TypeEntity> containedTypes;
    private List<FunctionEntity> containedFunctions;
    private List<PackageEntity> subPackages;
    private PackageEntity containingPackage;

    public PackageEntity(String id, String name, String lang, List<TypeEntity> containedTypes
            , List<FunctionEntity> containedFunctions, List<PackageEntity> subPackages
            , PackageEntity containingPackage) {
        this.id = id;
        this.name = name;
        this.lang = lang;
        this.containedTypes = containedTypes;
        this.containedFunctions = containedFunctions;
        this.subPackages = subPackages;
        this.containingPackage = containingPackage;
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

    public List<TypeEntity> getContainedTypes() {
        return containedTypes;
    }

    public List<FunctionEntity> getContainedFunctions() {
        return containedFunctions;
    }

    public List<PackageEntity> getSubPackages() {
        return subPackages;
    }

    public PackageEntity getContainingPackage() {
        return containingPackage;
    }
}
