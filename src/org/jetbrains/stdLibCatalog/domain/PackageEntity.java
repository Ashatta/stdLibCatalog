package org.jetbrains.stdLibCatalog.domain;

import java.util.List;

public class PackageEntity extends Entity {
    private final List<Classifier> containedClasses;
    private final List<FunctionEntity> containedFunctions;
    private final List<PackageEntity> subPackages;
    private PackageEntity containingPackage;

    public PackageEntity(String name, String lang, List<Classifier> containedClasses
            , List<FunctionEntity> containedFunctions, List<PackageEntity> subPackages, PackageEntity containingPackage
            , String documentation, String docLink) {
        super(name, lang, documentation, docLink);
        this.containedClasses = containedClasses;
        this.containedFunctions = containedFunctions;
        this.subPackages = subPackages;
        this.containingPackage = containingPackage;
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

    public void setContainingPackage(PackageEntity containingPackage) {
        this.containingPackage = containingPackage;
    }
}
