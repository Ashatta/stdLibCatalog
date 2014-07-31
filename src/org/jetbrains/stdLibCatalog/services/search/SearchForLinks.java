package org.jetbrains.stdLibCatalog.services.search;

import org.jetbrains.stdLibCatalog.domain.*;

import java.util.Collections;
import java.util.List;

public class SearchForLinks {
    public List<FunctionLink> findLinks(FunctionEntity func) {
        return Collections.emptyList();
    }

    public List<TypeLink> findLinks(TypeConstructor type) {
        return Collections.emptyList();
    }

    public List<FunctionLink> findLinkSuggestions(FunctionEntity func) {
        return Collections.emptyList();
    }

    public List<TypeLink> findLinkSuggestions(TypeConstructor type) {
        return Collections.emptyList();
    }
}
