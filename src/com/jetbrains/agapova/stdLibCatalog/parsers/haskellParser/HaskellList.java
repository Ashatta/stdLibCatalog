package com.jetbrains.agapova.stdLibCatalog.parsers.haskellParser;

/**
* Created by ashatta on 7/18/14.
*/
class HaskellList extends HaskellType {
    private HaskellType parameter;

    HaskellList() {}

    public static HaskellList parse(String signature) {
        if (!signature.startsWith("[") || !signature.endsWith("]")) {
            return null;
        }
        HaskellList list = new HaskellList();
        list.parameter = HaskellType.parse(signature.substring(1, signature.length() - 1));
        return (list.parameter == null ? null : list);
    }
}
