package com.jetbrains.agapova.stdLibCatalog.services.search;

import com.jetbrains.agapova.stdLibCatalog.domain.FunctionEntity;
import com.jetbrains.agapova.stdLibCatalog.domain.TypeEntity;

import java.util.Collections;
import java.util.List;

/**
 * Created by ashatta on 7/10/14.
 */
public class SearchByDoc {
    public List<FunctionEntity> findFunction(String lang, List<String> keywords) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<FunctionEntity> findFunction(String lang, String regexp) {
        return Collections.<FunctionEntity>emptyList();
    }

    // all methods for types search return both classes and interfaces
    public List<TypeEntity> findType(String lang, List<String> keywords) {
        return Collections.<TypeEntity>emptyList();
    }

    // all methods for types search return both classes and interfaces
    public List<TypeEntity> findType(String lang, String regexp) {
        return Collections.<TypeEntity>emptyList();

    }
}
