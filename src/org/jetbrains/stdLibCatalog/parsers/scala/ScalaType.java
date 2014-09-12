package org.jetbrains.stdLibCatalog.parsers.scala;

import org.jetbrains.stdLibCatalog.domain.Classifier;
import org.jetbrains.stdLibCatalog.domain.FunctionType;
import org.jetbrains.stdLibCatalog.domain.Type;
import org.jetbrains.stdLibCatalog.domain.TypeVariable;
import org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Map;

import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.QualifiedName;

public abstract class ScalaType {
    public static ScalaType parse(Element signatureElem, String signature) {
        ScalaFunction function = ScalaFunction.parse(signatureElem, signature);
        if (function != null) {
            return function;
        }

        ScalaTuple tuple = ScalaTuple.parse(signatureElem, signature);
        if (tuple != null) {
            return tuple;
        }

        ScalaWildcard wildcard = ScalaWildcard.parse(signatureElem, signature);
        if (wildcard != null) {
            return wildcard;
        }

        ScalaClass scalaClass = ScalaClass.parse(signatureElem, signature);
        if (scalaClass != null) {
            return scalaClass;
        }

        ScalaTypeVariable typeVariable = ScalaTypeVariable.parse(signatureElem, signature);
        if (typeVariable != null) {
            return typeVariable;
        }

        return null;
    }

    public abstract Classifier getClassifier(ScalaParser parser);

    public abstract Type buildType(ScalaParser parser, QualifiedName className, Map<String, TypeVariable> typeVariables);

    public FunctionType buildFunction(ScalaParser parser, QualifiedName className, Map<String, TypeVariable> typeVariables) {
        return new FunctionType(new ArrayList<Type>(), buildType(parser, className, typeVariables));
    }

    protected static String symbolNames(String str) {
        return str.replaceAll(":", "\\$colon")
                .replaceAll("&", "\\$amp")
                .replaceAll("\\+", "\\$plus")
                .replaceAll("<", "\\$less")
                .replaceAll(">", "\\$greater")
                .replaceAll("=", "\\$eq");
    }
}
