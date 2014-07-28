package org.jetbrains.stdLibCatalog.services.search;

import org.jetbrains.stdLibCatalog.domain.FunctionEntity;
import org.jetbrains.stdLibCatalog.domain.FunctionLink;
import org.jetbrains.stdLibCatalog.domain.TypeEntity;
import org.jetbrains.stdLibCatalog.domain.TypeLink;

import java.util.Collections;
import java.util.List;

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
