package org.jetbrains.stdLibCatalog.services.search;

import org.jetbrains.stdLibCatalog.domain.*;

import java.util.Collections;
import java.util.List;

public class SearchForConnectedEntities {
    public List<MemberEntity> findMemberFunctions(Classifier t) {
        return Collections.emptyList();
    }

    public List<Classifier> findSubtypes(Classifier t) {
        return Collections.emptyList();
    }

    public List<Classifier> findSupertypes(Classifier t) {
        return Collections.emptyList();
    }

    public List<MemberEntity> findUsingFunctions(Classifier t) {
        return Collections.emptyList();
    }

    public List<Classifier> findTypes(PackageEntity pack) {
        return Collections.emptyList();
    }

    public List<MemberEntity> findFunctions(PackageEntity pack) {
        return Collections.emptyList();
    }

    public Classifier findContainingType(MemberEntity func) {
        return null;
    }

    public PackageEntity findContainingPackage(MemberEntity func) {
        return null;
    }

    public PackageEntity findContainingPackage(Classifier t) {
        return null;
    }

    public PackageEntity findContainingPackage(PackageEntity pack) {
        return null;
    }

    public List<PackageEntity> findSubpackages(PackageEntity pack) {
        return Collections.emptyList();
    }

    public Type findAliasedType(TypeAlias alias) {
        return null;
    }

    public List<TypeAlias> findAliases(TypeConstructor t) {
        return Collections.emptyList();
    }
}
