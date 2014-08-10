package org.jetbrains.stdLibCatalog.domain;

import java.net.URL;
import java.util.List;

public class PackageEntity extends Entity {
    private final List<TypeConstructor> containedClasses;
    private final List<MemberEntity> containedFunctions;
    private final List<PackageEntity> subPackages;

    public PackageEntity(String name, Language lang, List<TypeConstructor> containedClasses,
            List<MemberEntity> containedFunctions, List<PackageEntity> subPackages, PackageEntity containingPackage,
            String documentation, URL docLink) {
        super(name, lang, documentation, docLink);
        this.containedClasses = containedClasses;
        this.containedFunctions = containedFunctions;
        this.subPackages = subPackages;
    }

    public List<MemberEntity> getContainedFunctions() {
        return containedFunctions;
    }

    public List<PackageEntity> getSubPackages() {
        return subPackages;
    }
}
