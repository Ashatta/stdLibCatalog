package org.jetbrains.stdLibCatalog.services.search;

import org.jetbrains.stdLibCatalog.domain.MemberEntity;
import org.jetbrains.stdLibCatalog.domain.FunctionType;

import java.util.Collections;
import java.util.List;

public class SearchBySignature {
    public List<MemberEntity> find(String lang, String name, FunctionType sign) {
        return Collections.emptyList();
    }

    public List<MemberEntity> findWithName(String name, FunctionType sign) {
        return Collections.emptyList();
    }

    public List<MemberEntity> findWithLang(String lang, FunctionType sign) {
        return Collections.emptyList();
    }
}
