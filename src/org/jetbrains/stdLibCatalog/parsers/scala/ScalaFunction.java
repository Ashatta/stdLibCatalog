package org.jetbrains.stdLibCatalog.parsers.scala;

import org.jetbrains.stdLibCatalog.domain.Classifier;
import org.jetbrains.stdLibCatalog.domain.FunctionType;
import org.jetbrains.stdLibCatalog.domain.Type;
import org.jetbrains.stdLibCatalog.domain.TypeVariable;
import org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScalaFunction extends ScalaType {
    private final List<ScalaType> parameters = new ArrayList<>();
    private ScalaType result;

    public static ScalaFunction parse(Element signatureElem, String signature) {
        if (signature.startsWith("(") && signature.endsWith(")") && ScalaParser.typeSplit(signature, "\u21D2").size() == 1) {
            signature = signature.substring(1, signature.length() - 1);
        }

        List<String> parts = ScalaParser.typeSplit(signature, "\u21D2");
        if (parts.size() < 2) {
            return null;
        }

        ScalaFunction result = new ScalaFunction();
        String paramsString = parts.get(0);
        if (paramsString.startsWith("(") && paramsString.endsWith(")")) {
            paramsString = paramsString.substring(1, paramsString.length() - 1);
        }

        if (!paramsString.isEmpty()) {
            List<String> params = ScalaParser.typeSplit(paramsString, ",");
            for (String paramString : params) {
                ScalaType param = ScalaType.parse(signatureElem, paramString);
                if (param == null) {
                    return null;
                }

                result.parameters.add(param);
            }
        }

        result.result = ScalaType.parse(signatureElem, StringUtil.join(parts.subList(1, parts.size()), "\u21D2"));
        return (result.result == null ? null : result);
    }

    public Classifier getClassifier(ScalaParser parser) {
        return null;
    }

    public FunctionType buildType(ScalaParser parser, ParserUtils.QualifiedName className, Map<String, TypeVariable> typeVariables) {
        List<Type> params = new ArrayList<>();
        for (ScalaType param : parameters) {
            params.add(param.buildType(parser, className, typeVariables));
        }

        return new FunctionType(params, result.buildType(parser, className, typeVariables));
    }

    public FunctionType buildFunction(ScalaParser parser, ParserUtils.QualifiedName className, Map<String, TypeVariable> typeVariables) {
        return buildType(parser, className, typeVariables);
    }
}
