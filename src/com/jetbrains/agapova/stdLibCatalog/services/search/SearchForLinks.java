package com.jetbrains.agapova.stdLibCatalog.services.search;

import com.jetbrains.agapova.stdLibCatalog.domain.FunctionEntity;
import com.jetbrains.agapova.stdLibCatalog.domain.FunctionLink;
import com.jetbrains.agapova.stdLibCatalog.domain.TypeEntity;
import com.jetbrains.agapova.stdLibCatalog.domain.TypeLink;

import java.util.Collections;
import java.util.List;

/**
 * Created by ashatta on 7/10/14.
 */
public class SearchForLinks {
    public List<FunctionLink> findLinks(FunctionEntity func) {
        return Collections.<FunctionLink>emptyList();
    }

    public List<TypeLink> findLinks(TypeEntity type) {
        return Collections.<TypeLink>emptyList();
    }

    public List<FunctionLink> findLinkSuggestions(FunctionEntity func) {
        return Collections.<FunctionLink>emptyList();
    }

    public List<TypeLink> findLinkSuggestions(TypeEntity type) {
        return Collections.<TypeLink>emptyList();
    }
}
