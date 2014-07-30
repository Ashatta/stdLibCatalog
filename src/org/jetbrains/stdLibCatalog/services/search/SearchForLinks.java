package org.jetbrains.stdLibCatalog.services.search;

import org.jetbrains.stdLibCatalog.domain.*;

import java.util.Collections;
import java.util.List;

public class SearchForLinks {
    public List<FunctionLink> findLinks(FunctionEntity func) {
        return Collections.emptyList();
    }

    public List<TypeLink> findLinks(Classifier type) {
        return Collections.emptyList();
    }

    public List<FunctionLink> findLinkSuggestions(FunctionEntity func) {
        return Collections.emptyList();
    }

    public List<TypeLink> findLinkSuggestions(Classifier type) {
        return Collections.emptyList();
    }
}
