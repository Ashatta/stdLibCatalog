package org.jetbrains.stdLibCatalog.services.search;

import org.jetbrains.stdLibCatalog.domain.Classifier;
import org.jetbrains.stdLibCatalog.domain.FunctionEntity;

import java.util.Collections;
import java.util.List;

public class SearchByDoc {
    public List<FunctionEntity> findFunction(String lang, List<String> keywords) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<FunctionEntity> findFunction(String lang, String regexp) {
        return Collections.<FunctionEntity>emptyList();
    }

    // all methods for types search return both classes and interfaces
    public List<Classifier> findType(String lang, List<String> keywords) {
        return Collections.<Classifier>emptyList();
    }

    // all methods for types search return both classes and interfaces
    public List<Classifier> findType(String lang, String regexp) {
        return Collections.<Classifier>emptyList();

    }
}
