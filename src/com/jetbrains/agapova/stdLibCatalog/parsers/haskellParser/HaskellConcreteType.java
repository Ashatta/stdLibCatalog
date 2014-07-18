package com.jetbrains.agapova.stdLibCatalog.parsers.haskellParser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* Created by ashatta on 7/18/14.
*/
class HaskellConcreteType extends HaskellType {
    private String name;
    private List<HaskellType> parameters;

    HaskellConcreteType() {
        parameters = new ArrayList<>();
    }

    public static HaskellConcreteType parse(String signature) {
        if (signature.startsWith("(") && signature.endsWith(")")) {
            signature = signature.substring(1, signature.length() - 1);
        }

        if (!Character.isUpperCase(signature.charAt(0))) {
            return null;
        }

        HaskellConcreteType concrete = new HaskellConcreteType();
        List<String> params = typeSplit(signature, "");
        Iterator<String> it = params.iterator();
        concrete.name = it.next();
        while (it.hasNext()) {
            String par = it.next();
            HaskellType param = HaskellType.parse(par);
            if (param == null) {
                return null;
            }
            concrete.parameters.add(param);
        }

        return concrete;
    }

    public String getName() {
        return name;
    }

    public List<HaskellType> getParameters() {
        return parameters;
    }
}
