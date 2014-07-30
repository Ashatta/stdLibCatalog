package org.jetbrains.stdLibCatalog.services.search;

import org.jetbrains.stdLibCatalog.domain.Classifier;
import org.jetbrains.stdLibCatalog.domain.FunctionEntity;
import org.jetbrains.stdLibCatalog.domain.PackageEntity;

import java.util.Collections;
import java.util.List;

public class SearchByName {
    public List<FunctionEntity> findFunction(String name) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<FunctionEntity> findFunction(String lang, String name) {
        return Collections.emptyList();
    }

    public List<FunctionEntity> findFunctionWithType(String name, String type) {
        return Collections.emptyList();
    }

    public List<FunctionEntity> findFunctionWithType(String lang, String name, String type) {
        return Collections.emptyList();
    }

    public List<FunctionEntity> findFunctionWithTypePackage(String name, String type, String pack) {
        return Collections.emptyList();
    }

    public List<FunctionEntity> findFunctionWithTypePackage(String lang, String name, String type, String pack) {
        return Collections.emptyList();
    }

    public List<Classifier> findType(String name) {
        return Collections.emptyList();
    }

    public List<Classifier> findType(String lang, String name) {
        return Collections.emptyList();
    }

    public List<Classifier> findTypeWithPackage(String name, String pack) {
        return Collections.emptyList();
    }

    public List<Classifier> findTypeWithPackage(String lang, String name, String pack) {
        return Collections.emptyList();
    }

    public List<PackageEntity> findPackage(String name) {
        return Collections.emptyList();
    }

    public List<PackageEntity> findPackage(String lang, String name) {
        return Collections.emptyList();
    }
}
