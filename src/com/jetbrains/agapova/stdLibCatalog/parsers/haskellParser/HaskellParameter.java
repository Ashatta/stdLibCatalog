package com.jetbrains.agapova.stdLibCatalog.parsers.haskellParser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* Created by ashatta on 7/18/14.
*/
class HaskellParameter extends HaskellType {
    private String name;
    private List<HaskellType> parameters;

    HaskellParameter() {
        parameters = new ArrayList<>();
    }

    public static HaskellParameter parse(String signature) {
        if (signature.startsWith("(") && signature.endsWith(")")) {
            signature = signature.substring(1, signature.length() - 1);
        }

        if (!Character.isLowerCase(signature.charAt(0))) {
            return null;
        }

        HaskellParameter parameter = new HaskellParameter();
        List<String> params = typeSplit(signature, "");
        Iterator<String> it = params.iterator();
        parameter.name = it.next();
        while (it.hasNext()) {
            String par = it.next();
            HaskellType param = HaskellType.parse(par);
            if (param == null) {
                return null;
            }
            parameter.parameters.add(param);
        }

        return parameter;
    }

    public List<HaskellType> getParameters() {
        return parameters;
    }
}
