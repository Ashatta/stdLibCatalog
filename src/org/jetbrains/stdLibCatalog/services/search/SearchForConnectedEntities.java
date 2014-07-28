package org.jetbrains.stdLibCatalog.services.search;

import org.jetbrains.stdLibCatalog.domain.*;

import java.util.Collections;
import java.util.List;

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

    public List<ClassEntity> findSupportingClasses(InterfaceEntity t) {
        return Collections.<ClassEntity>emptyList();
    }

    public List<InterfaceEntity> findInterfaces(ClassEntity t) {
        return Collections.<InterfaceEntity>emptyList();
    }

    public List<FunctionEntity> findUsingFunctions(TypeEntity t) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<TypeEntity> findTypes(PackageEntity pack) {
        return Collections.<TypeEntity>emptyList();
    }

    public List<ClassEntity> findClasses(PackageEntity pack) {
        return Collections.<ClassEntity>emptyList();
    }

    public List<InterfaceEntity> findInterfaces(PackageEntity pack) {
        return Collections.<InterfaceEntity>emptyList();
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
