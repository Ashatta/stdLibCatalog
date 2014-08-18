package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.*;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

class HaskellTuple extends HaskellType {
    private List<HaskellType> parameters;

    HaskellTuple() {
        parameters = new ArrayList<>();
    }

    public static HaskellTuple parse(Element elem, String signature, List<HaskellConstraint> parameters) {
        if (!signature.startsWith("(") || !signature.endsWith(")")) {
            return null;
        }
        signature = signature.substring(1, signature.length() - 1);
        boolean singletonAllowed = false;
        if (signature.startsWith("#") && signature.endsWith("#")) {
            singletonAllowed = true;
            signature = signature.substring(1, signature.length() - 1);
        }

        HaskellTuple tuple = new HaskellTuple();
        List<String> args = typeSplit(signature, ",");
        if (args.size() == 1 && !singletonAllowed) {
            return null;
        }

        for (String arg : args) {
            if (!arg.startsWith("(") || !arg.endsWith(")")) {
                arg = "(" + arg + ")";
            }
            HaskellType a = HaskellType.parse(elem, arg, parameters);
            if (a == null) {
                return null;
            }
            tuple.parameters.add(a);
        }

        return tuple;
    }

    public DataType buildType(HaskellParser parser, HaskellParser.QualifiedName entity, boolean isType) {
        String tupleName = "(" + new String(new char[parameters.size() == 0 ? 0 : parameters.size() - 1])
                .replace("\0", ",") + ")";
        Classifier tuple = parser.classes.get(new HaskellParser.QualifiedName(
                parameters.size() > 62 ? HaskellParser.OTHER_PACKAGE : "GHC.Tuple", tupleName));
        List<Type> parameters = new ArrayList<>();
        for (HaskellType type : this.parameters) {
            parameters.add(type.buildType(parser, entity, isType));
        }

        return new DataType(tuple, parameters);
    }

    public void extractVariables(List<String> variables, int paramsNumber) {
        for (HaskellType param : parameters) {
            param.extractVariables(variables, 0);
        }
    }

    public String getName() {
        return "(" + new String(new char[parameters.size() == 0 ? 0 : parameters.size() + 1]).replace("\0", ",") + ")";
    }

    protected String classifierName() {
        String result = "(" + new String(new char[parameters.size() == 0 ? 0 : parameters.size() + 1])
                .replace("\0", ",") + ")" + "<";
        for (HaskellType param : parameters) {
            result += param.classifierName() + ",";
        }
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1);
        }
        return result + ">";
    }
}
