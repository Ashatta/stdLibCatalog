package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.FunctionType;
import org.jetbrains.stdLibCatalog.domain.Type;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class HaskellFunction extends HaskellType {
    private List<HaskellType> arguments;

    HaskellFunction() {
        arguments = new ArrayList<>();
    }

    public static HaskellFunction parse(Element elem, String signature, List<HaskellConstraint> constraints) {
        if (!signature.startsWith("(") || !signature.endsWith(")")) {
            return null;
        }
        signature = signature.substring(1, signature.length() - 1);

        HaskellFunction func = new HaskellFunction();

        List<HaskellConstraint> localConstraints = new ArrayList<>();
        List<String> types = typeSplit(signature, "->");
        for (String arg : types) {
            List<String> parts = typeSplit(arg, "=>");
            if (parts.size() > 1) {
                HaskellConstraint.parseConstraints(elem, parts.get(0), localConstraints);
            }

            HaskellType a = HaskellType.parse(elem, parts.get(parts.size() - 1), localConstraints);
            if (a == null) {
                return null;
            }
            func.arguments.add(a);
        }

        constraints.addAll(localConstraints);
        return func;
    }

    public FunctionType makeSignature(HaskellParser parser, HaskellParser.QualifiedName entity, boolean isType) {
        Iterator<HaskellType> it = arguments.iterator();
        List<Type> args = new ArrayList<>();
        HaskellType type = null;
        while (it.hasNext()) {
            type = it.next();
            if (!it.hasNext()) {
                break;
            }

            args.add(type.buildType(parser, entity, isType));
        }

        assert type != null;
        return new FunctionType(args, type.buildType(parser, entity, isType));
    }

    public FunctionType buildType(HaskellParser parser, HaskellParser.QualifiedName entity, boolean isType) {
        return makeSignature(parser, entity, isType);
    }

    public void extractVariables(List<String> variables, int paramsNumber) {
        for (HaskellType param : arguments) {
            param.extractVariables(variables, 0);
        }
    }

    public String getName() {
        return "";
    }

    protected String classifierName() {
        String result = "Function<";
        for (HaskellType param : arguments) {
            result += param.classifierName() + ",";
        }
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1);
        }
        return result + ">";
    }
}
