package com.jetbrains.agapova.stdLibCatalog.parsers.haskellParser;

import com.jetbrains.agapova.stdLibCatalog.domain.ClassEntity;
import com.jetbrains.agapova.stdLibCatalog.domain.FunctionEntity;
import com.jetbrains.agapova.stdLibCatalog.domain.TypedEntity;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
* Created by ashatta on 7/18/14.
*/
class HaskellTuple extends HaskellType {
    private List<HaskellType> subs;

    HaskellTuple() {
        subs = new ArrayList<>();
    }

    public static HaskellTuple parse(String signature, Map<String, Pair<Integer, List<Pair<String, String>>>> parameters) {
        if (!signature.startsWith("(") || !signature.endsWith(")")) {
            return null;
        }

        HaskellTuple tuple = new HaskellTuple();
        List<String> args = typeSplit(signature.substring(1, signature.length() - 1), ",");
        if (args.size() == 1) {
            return null;
        }

        for (String arg : args) {
            if (!arg.startsWith("(") || !arg.endsWith(")")) {
                arg = "(" + arg + ")";
            }
            HaskellType a = HaskellType.parse(arg, parameters);
            if (a == null) {
                return null;
            }
            tuple.subs.add(a);
        }

        return tuple;
    }

    public List<HaskellType> getSubs() {
        return subs;
    }

    public ClassEntity buildType(HaskellParser parser, FunctionEntity function) {
        ClassEntity tuple = parser.classes.get("other").get("Tuple" + String.valueOf(subs.size())).clone();
        List<TypedEntity> parameters = new ArrayList<>();
        for (HaskellType type : subs) {
            parameters.add(type.buildType(parser, function));
        }

        tuple.setParameters(parameters);
        return tuple;
    }

    public void addParameters(HaskellParser parser, FunctionEntity function, List<TypedEntity> params) {
        for (HaskellType type : subs) {
            type.addParameters(parser, function, params);
        }
    }
}
