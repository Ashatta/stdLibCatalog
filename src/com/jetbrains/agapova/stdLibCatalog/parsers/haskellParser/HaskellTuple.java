package com.jetbrains.agapova.stdLibCatalog.parsers.haskellParser;

import java.util.ArrayList;
import java.util.List;

/**
* Created by ashatta on 7/18/14.
*/
class HaskellTuple extends HaskellType {
    private List<HaskellType> subs;

    HaskellTuple() {
        subs = new ArrayList<>();
    }

    public static HaskellTuple parse(String signature) {
        if (!signature.startsWith("(") || !signature.endsWith(")")) {
            return null;
        }

        HaskellTuple tuple = new HaskellTuple();
        for (String arg : typeSplit(signature.substring(1, signature.length() - 1), ",")) {
            HaskellType a = HaskellType.parse(arg);
            if (a == null) {
                return null;
            }
            tuple.subs.add(a);
        }

        if (tuple.subs.size() == 1) {
            return null;
        }

        return tuple;
    }

    public List<HaskellType> getSubs() {
        return subs;
    }
}
