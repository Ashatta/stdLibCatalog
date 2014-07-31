package org.jetbrains.stdLibCatalog.domain;

import java.util.List;

public class PackageEntity extends Entity {
    private final List<TypeConstructor> containedClasses;
    private final List<FunctionEntity> containedFunctions;
    private final List<PackageEntity> subPackages;

    public PackageEntity(String name, String lang, List<TypeConstructor> containedClasses
            , List<FunctionEntity> containedFunctions, List<PackageEntity> subPackages, PackageEntity containingPackage
            , String documentation, String docLink) {
        super(name, lang, documentation, docLink);
        this.containedClasses = containedClasses;
        this.containedFunctions = containedFunctions;
        this.subPackages = subPackages;
    }

    public List<FunctionEntity> getContainedFunctions() {
        return containedFunctions;
    }

    public List<PackageEntity> getSubPackages() {
        return subPackages;
    }
}
