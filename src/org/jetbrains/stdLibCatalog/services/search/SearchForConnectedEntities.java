package org.jetbrains.stdLibCatalog.services.search;

import org.jetbrains.stdLibCatalog.domain.*;

import java.util.Collections;
import java.util.List;

public class SearchForConnectedEntities {
    public List<FunctionEntity> findMemberFunctions(Classifier t) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<Classifier> findSubtypes(Classifier t) {
        return Collections.<Classifier>emptyList();
    }

    public List<Classifier> findSupertypes(Classifier t) {
        return Collections.<Classifier>emptyList();
    }

    public List<Classifier> findSupportingClasses(Classifier t) {
        return Collections.<Classifier>emptyList();
    }

    public List<Classifier> findInterfaces(Classifier t) {
        return Collections.<Classifier>emptyList();
    }

    public List<FunctionEntity> findUsingFunctions(Classifier t) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<Classifier> findClassifiers(PackageEntity pack) {
        return Collections.<Classifier>emptyList();
    }

    public List<FunctionEntity> findFunctions(PackageEntity pack) {
        return Collections.<FunctionEntity>emptyList();
    }

    public Classifier findContainingType(FunctionEntity func) {
        return null;
    }

    public PackageEntity findContainingPackage(FunctionEntity func) {
        return null;
    }

    public PackageEntity findContainingPackage(Classifier t) {
        return null;
    }
}
