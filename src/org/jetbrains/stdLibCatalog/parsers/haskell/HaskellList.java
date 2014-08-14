package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class HaskellList extends HaskellType {
    private HaskellType parameter;

    HaskellList() {}

    public static HaskellList parse(String signature, Map<String, HaskellParser.ParameterDescription> parameters) {
        if (!signature.startsWith("[") || !signature.endsWith("]")) {
            return null;
        }
        HaskellList list = new HaskellList();

        String arg = signature.substring(1, signature.length() - 1);
        if (arg.isEmpty()) {
            list.parameter = HaskellType.parse("curriedParam", parameters);
            return list;
        }

        if (!arg.startsWith("(") || !arg.endsWith(")")) {
            arg = "(" + arg + ")";
        }
        list.parameter = HaskellType.parse(arg, parameters);

        return (list.parameter == null ? null : list);
    }

    public DataType buildType(HaskellParser parser, HaskellParser.QualifiedName entity, boolean isType) {
        Classifier list = parser.classes.get(new HaskellParser.QualifiedName("other", "List"));
        List<Type> parameters = new ArrayList<>();
        parameters.add(parameter.buildType(parser, entity, isType));
        return new DataType(list, parameters);
    }
}
