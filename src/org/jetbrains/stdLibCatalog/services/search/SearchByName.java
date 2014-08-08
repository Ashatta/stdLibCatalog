package org.jetbrains.stdLibCatalog.services.search;

import org.jetbrains.stdLibCatalog.domain.Classifier;
import org.jetbrains.stdLibCatalog.domain.MemberEntity;
import org.jetbrains.stdLibCatalog.domain.PackageEntity;

import java.util.Collections;
import java.util.List;

public class SearchByName {
    public List<MemberEntity> findFunction(String name) {
        return Collections.<MemberEntity>emptyList();
    }

    public List<MemberEntity> findFunction(String lang, String name) {
        return Collections.emptyList();
    }

    public List<MemberEntity> findFunctionWithType(String name, String type) {
        return Collections.emptyList();
    }

    public List<MemberEntity> findFunctionWithType(String lang, String name, String type) {
        return Collections.emptyList();
    }

    public List<MemberEntity> findFunctionWithTypePackage(String name, String type, String pack) {
        return Collections.emptyList();
    }

    public List<MemberEntity> findFunctionWithTypePackage(String lang, String name, String type, String pack) {
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
