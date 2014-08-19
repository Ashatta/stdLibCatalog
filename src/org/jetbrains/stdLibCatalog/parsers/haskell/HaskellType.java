package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.*;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

abstract class HaskellType {
    public static HaskellType parse(Element elem, String signature, List<HaskellConstraint> constraints) {
        HaskellList list = HaskellList.parse(elem, signature, constraints);
        if (list != null) {
            return list;
        }

        HaskellTuple tuple = HaskellTuple.parse(elem, signature, constraints);
        if (tuple != null) {
            return tuple;
        }

        HaskellConcreteType concrete = HaskellConcreteType.parse(elem, signature, constraints);
        if (concrete != null) {
            return concrete;
        }

        HaskellParameter param = HaskellParameter.parse(elem, signature, constraints);
        if (param != null) {
            return param;
        }

        HaskellFunction func = HaskellFunction.parse(elem, signature, constraints);
        if (func != null) {
            return func;
        }

        return null;
    }

    static List<String> typeSplit(String signature, String str) {
        List<String> result = new ArrayList<>();
        int braces = 0;
        int start = 0;
        int len = str.length();
        for (int i = 0; i < signature.length(); ++i) {
            i = skip(signature, i, len);

            if (signature.charAt(i) == '(' || signature.charAt(i) == '[') {
                braces++;
            } else if (signature.charAt(i) == ')' || signature.charAt(i) == ']') {
                braces--;
            } else if (braces == 0 && substr(signature, str, i, len)) {
                result.add(signature.substring(start, i).trim());
                i += Math.max(len - 1, 0);
                start = i + 1;
            }
        }

        String last = signature.substring(start, signature.length()).trim();
        if (!last.isEmpty()) {
            result.add(last);
        }

        return result;
    }

    private static boolean substr(String signature, String str, int i, int len) {
        if (len == 0) {
            return Character.isWhitespace(signature.charAt(i));
        } else {
            return  i + len < signature.length() && signature.substring(i, i + len).equals(str);
        }
    }

    private static int skip(String signature, int i, int len) {
        if (len != 0) {
            while (Character.isWhitespace(signature.charAt(i))) {
                ++i;
            }
        }

        return i;
    }

    public FunctionType makeSignature(HaskellParser parser, HaskellParser.QualifiedName entity, boolean isType) {
        return new FunctionType(new ArrayList<Type>(), buildType(parser, entity, isType));
    }

    abstract public Type buildType(HaskellParser parser, HaskellParser.QualifiedName entity, boolean isType);

    public Classifier buildClassifier(String definition, HaskellParser.QualifiedName typeClass, int paramsNumber) {
        Classifier fakeClassifier = new Classifier(
                typeClass.getKey() + "." + typeClass.getValue() + "_" + classifierName(), Language.HASKELL, "", null,
                new ArrayList<MemberEntity>(), definition);

        List<String> variables = new ArrayList<>();
        extractVariables(variables, paramsNumber);
        for (String variable : variables) {
            fakeClassifier.addParameter(new TypeVariable(variable, Language.HASKELL));
        }

        return fakeClassifier;

    }

    public abstract void extractVariables(List<String> variables, int paramsNumber);

    public abstract String getName();

    protected abstract String classifierName();
}
