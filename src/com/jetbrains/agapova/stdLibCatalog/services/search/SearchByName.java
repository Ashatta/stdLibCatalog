package com.jetbrains.agapova.stdLibCatalog.services.search;

import com.jetbrains.agapova.stdLibCatalog.domain.FunctionEntity;
import com.jetbrains.agapova.stdLibCatalog.domain.PackageEntity;
import com.jetbrains.agapova.stdLibCatalog.domain.TypeEntity;

import java.util.List;
import java.util.Collections;

/**
 * Created by ashatta on 7/10/14.
 */
public class SearchByName {
    public List<FunctionEntity> findFunction(String name) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<FunctionEntity> findFunction(String lang, String name) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<FunctionEntity> findFunctionWithType(String name, String type) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<FunctionEntity> findFunctionWithType(String lang, String name, String type) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<FunctionEntity> findFunctionWithTypePackage(String name, String type, String pack) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<FunctionEntity> findFunctionWithTypePackage(String lang, String name, String type, String pack) {
        return Collections.<FunctionEntity>emptyList();
    }

    // all methods for types search return both classes and interfaces
    public List<TypeEntity> findType(String name) {
        return Collections.<TypeEntity>emptyList();
    }

    // all methods for types search return both classes and interfaces
    public List<TypeEntity> findType(String lang, String name) {
        return Collections.<TypeEntity>emptyList();
    }

    // all methods for types search return both classes and interfaces
    public List<TypeEntity> findTypeWithPackage(String name, String pack) {
        return Collections.<TypeEntity>emptyList();
    }

    // all methods for types search return both classes and interfaces
    public List<TypeEntity> findTypeWithPackage(String lang, String name, String pack) {
        return Collections.<TypeEntity>emptyList();
    }

    // all methods for types search return both classes and interfaces
    public List<PackageEntity> findPackage(String name) {
        return Collections.<PackageEntity>emptyList();
    }

    // all methods for types search return both classes and interfaces
    public List<PackageEntity> findPackage(String lang, String name) {
        return Collections.<PackageEntity>emptyList();
    }
}
