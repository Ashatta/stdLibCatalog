package org.jetbrains.stdLibCatalog.parsers.haskell;

import javafx.util.Pair;
import org.jetbrains.stdLibCatalog.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public DataType buildType(HaskellParser parser, FunctionEntity function) {
        Classifier tuple = parser.classes.get("other").get("Tuple" + String.valueOf(subs.size()));
        List<TypeEntity> parameters = new ArrayList<>();
        for (HaskellType type : subs) {
            parameters.add(type.buildType(parser, function));
        }

        return new DataType(tuple, parameters);
    }
}