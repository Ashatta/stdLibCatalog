package org.jetbrains.stdLibCatalog.services.search;

import org.jetbrains.stdLibCatalog.domain.FunctionEntity;
import org.jetbrains.stdLibCatalog.domain.FunctionType;

import java.util.Collections;
import java.util.List;

public class SearchBySignature {
    public List<FunctionEntity> find(String lang, String name, FunctionType sign) {
        return Collections.emptyList();
    }

    public List<FunctionEntity> findWithName(String name, FunctionType sign) {
        return Collections.emptyList();
    }

    public List<FunctionEntity> findWithLang(String lang, FunctionType sign) {
        return Collections.emptyList();
    }
}
