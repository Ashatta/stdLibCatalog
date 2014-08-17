package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.*;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

class HaskellList extends HaskellType {
    private HaskellType parameter;

    HaskellList() {}

    public static HaskellList parse(Element elem, String signature, List<HaskellConstraint> parameters) {
        if (!signature.startsWith("[") || !signature.endsWith("]")) {
            return null;
        }
        HaskellList list = new HaskellList();

        String arg = signature.substring(1, signature.length() - 1);
        if (arg.isEmpty()) {
            list.parameter = HaskellType.parse(elem, "curriedParam", parameters);
            return list;
        }

        if (!arg.startsWith("(") || !arg.endsWith(")")) {
            arg = "(" + arg + ")";
        }
        list.parameter = HaskellType.parse(elem, arg, parameters);

        return (list.parameter == null ? null : list);
    }

    public DataType buildType(HaskellParser parser, HaskellParser.QualifiedName entity, boolean isType) {
        Classifier list = parser.classes.get(new HaskellParser.QualifiedName("other", "List"));
        List<Type> parameters = new ArrayList<>();
        parameters.add(parameter.buildType(parser, entity, isType));
        return new DataType(list, parameters);
    }

    public void extractVariables(List<String> variables, int paramsNumber) {
        parameter.extractVariables(variables, 0);
    }

    public String getName() {
        return "List";
    }

    protected String classifierName() {
        return "List<" + parameter.classifierName() + ">";
    }
}
