package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.*;
import org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.QualifiedName;

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
        return ParserUtils.typeSplit(signature, str, Arrays.asList('(', '['), Arrays.asList(')', ']'));
    }

    public FunctionType makeSignature(HaskellParser parser, QualifiedName entity, boolean isType) {
        return new FunctionType(new ArrayList<Type>(), buildType(parser, entity, isType));
    }

    abstract public Type buildType(HaskellParser parser, QualifiedName entity, boolean isType);

    public Classifier buildClassifier(String definition, QualifiedName typeClass, int paramsNumber) {
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
