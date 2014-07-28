package org.jetbrains.stdLibCatalog.services.search;

import org.jetbrains.stdLibCatalog.domain.FunctionEntity;
import org.jetbrains.stdLibCatalog.domain.Signature;

import java.util.Collections;
import java.util.List;

public class SearchBySignature {
    public List<FunctionEntity> find(String lang, String name, Signature sign) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<FunctionEntity> findWithName(String name, Signature sign) {
        return Collections.<FunctionEntity>emptyList();
    }

    public List<FunctionEntity> findWithLang(String lang, Signature sign) {
        return Collections.<FunctionEntity>emptyList();
    }
}
