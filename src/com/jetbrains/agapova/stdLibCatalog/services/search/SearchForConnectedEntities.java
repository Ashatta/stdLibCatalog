package com.jetbrains.agapova.stdLibCatalog.services.search;

import com.jetbrains.agapova.stdLibCatalog.domain.FunctionEntity;
import com.jetbrains.agapova.stdLibCatalog.domain.TypeEntity;
import com.jetbrains.agapova.stdLibCatalog.domain.PackageEntity;

import java.util.Collections;
import java.util.List;

/**
 * Created by ashatta on 7/10/14.
 */
public class SearchForConnectedEntities {
    public List<FunctionEntity> findMemberFunctions(TypeEntity t) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<TypeEntity> findSubtypes(TypeEntity t) {
        return Collections.<TypeEntity>emptyList();
    }

    public List<TypeEntity> findSupertypes(TypeEntity t) {
        return Collections.<TypeEntity>emptyList();
    }

    public List<FunctionEntity> findUsingFunctions(TypeEntity t) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<TypeEntity> findTypes(PackageEntity pack) {
        return Collections.<TypeEntity>emptyList();
    }

    public List<FunctionEntity> findFunctions(PackageEntity pack) {
        return Collections.<FunctionEntity>emptyList();
    }

    public TypeEntity findContainingType(FunctionEntity func) {
        return null;
    }

    public PackageEntity findContainingPackage(FunctionEntity func) {
        return null;
    }

    public PackageEntity findContainingPackage(TypeEntity t) {
        return null;
    }
}
