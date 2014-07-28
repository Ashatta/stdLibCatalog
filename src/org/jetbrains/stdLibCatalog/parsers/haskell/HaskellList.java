package org.jetbrains.stdLibCatalog.parsers.haskell;

import javafx.util.Pair;
import org.jetbrains.stdLibCatalog.domain.ClassEntity;
import org.jetbrains.stdLibCatalog.domain.FunctionEntity;
import org.jetbrains.stdLibCatalog.domain.TypedEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class HaskellList extends HaskellType {
    private HaskellType parameter;

    HaskellList() {}

    public static HaskellList parse(String signature, Map<String, Pair<Integer, List<Pair<String, String>>>> parameters) {
        if (!signature.startsWith("[") || !signature.endsWith("]")) {
            return null;
        }
        HaskellList list = new HaskellList();
        String arg = signature.substring(1, signature.length() - 1);
        if (!arg.startsWith("(") || !arg.endsWith(")")) {
            arg = "(" + arg + ")";
        }
        list.parameter = HaskellType.parse(arg, parameters);
        return (list.parameter == null ? null : list);
    }

    public ClassEntity buildType(HaskellParser parser, FunctionEntity function) {
        ClassEntity list = parser.classes.get("other").get("List").clone();
        List<TypedEntity> parameters = new ArrayList<>();
        parameters.add(parameter.buildType(parser, function));
        list.setParameters(parameters);
        return list;
    }

    public void addParameters(HaskellParser parser, FunctionEntity function, List<TypedEntity> params) {
        parameter.addParameters(parser, function, params);
    }

}
