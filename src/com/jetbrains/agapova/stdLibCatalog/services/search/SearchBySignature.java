package com.jetbrains.agapova.stdLibCatalog.services.search;

import com.jetbrains.agapova.stdLibCatalog.domain.FunctionEntity;
import com.jetbrains.agapova.stdLibCatalog.domain.Signature;

import java.util.Collections;
import java.util.List;

/**
 * Created by ashatta on 7/10/14.
 */
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
