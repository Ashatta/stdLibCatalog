package com.jetbrains.agapova.stdLibCatalog.domain;

import java.util.List;

/**
 * Created by ashatta on 7/10/14.
 */
public class PackageEntity extends Entity {
    private List<ClassEntity> containedClasses;
    private List<ClassEntity> containedInterfaces;
    private List<FunctionEntity> containedFunctions;
    private List<PackageEntity> subPackages;
    private PackageEntity containingPackage;

    public PackageEntity(String id, String name, String lang, List<ClassEntity> containedClasses
            , List<ClassEntity> containedInterfaces, List<FunctionEntity> containedFunctions
            , List<PackageEntity> subPackages, PackageEntity containingPackage, String documentation, String docLink) {
        super(id, lang, name, documentation, docLink);
        this.containedClasses = containedClasses;
        this.containedInterfaces = containedInterfaces;
        this.containedFunctions = containedFunctions;
        this.subPackages = subPackages;
        this.containingPackage = containingPackage;
    }

    public List<ClassEntity> getContainedClasses() {
        return containedClasses;
    }

    public List<ClassEntity> getContainedInterfaces() {
        return containedInterfaces;
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
