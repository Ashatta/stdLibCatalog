package org.jetbrains.stdLibCatalog.services.search;

import org.jetbrains.stdLibCatalog.domain.Classifier;
import org.jetbrains.stdLibCatalog.domain.MemberEntity;

import java.util.Collections;
import java.util.List;

public class SearchByDoc {
    public List<MemberEntity> findFunction(String lang, List<String> keywords) {
        return Collections.emptyList();
    }

    public List<MemberEntity> findFunction(String lang, String regexp) {
        return Collections.emptyList();
    }

    public List<Classifier> findType(String lang, List<String> keywords) {
        return Collections.emptyList();
    }

     public List<Classifier> findType(String lang, String regexp) {
        return Collections.emptyList();

    }
}
