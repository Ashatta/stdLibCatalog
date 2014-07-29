package org.jetbrains.stdLibCatalog.parsers.haskell;

import javafx.util.Pair;
import org.jetbrains.stdLibCatalog.domain.*;

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

    public DataType buildType(HaskellParser parser, FunctionEntity function) {
        Classifier list = parser.classes.get("other").get("List");
        List<TypeEntity> parameters = new ArrayList<>();
        parameters.add(parameter.buildType(parser, function));
        return new DataType(list, parameters);
    }
}
