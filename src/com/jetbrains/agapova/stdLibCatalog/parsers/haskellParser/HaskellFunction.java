package com.jetbrains.agapova.stdLibCatalog.parsers.haskellParser;

import java.util.ArrayList;
import java.util.List;

/**
* Created by ashatta on 7/18/14.
*/
class HaskellFunction extends HaskellType {
    private List<HaskellType> arguments;

    HaskellFunction() {
        arguments = new ArrayList<>();
    }

    public static HaskellFunction parse(String signature) {
        if (!signature.startsWith("(") || !signature.endsWith(")")) {
            return null;
        }

        signature = signature.substring(1, signature.length() - 1);
        String[] parts = signature.split("\\.\\s+");
        signature = parts[parts.length - 1];

        HaskellFunction func = new HaskellFunction();
        List<String> types = typeSplit(signature, "->");
        if (types.size() < 2) {
            return null;
        }

        for (String arg : types) {
            HaskellType a = HaskellType.parse(arg);
            if (a == null) {
                return null;
            }
            func.arguments.add(a);
        }

        return func;
    }

    public List<HaskellType> getArguments() {
        return arguments;
    }
}
