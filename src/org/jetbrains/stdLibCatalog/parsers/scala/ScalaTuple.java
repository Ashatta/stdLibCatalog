package org.jetbrains.stdLibCatalog.parsers.scala;

import org.jetbrains.stdLibCatalog.domain.DataType;
import org.jetbrains.stdLibCatalog.domain.Type;
import org.jetbrains.stdLibCatalog.domain.TypeVariable;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.QualifiedName;

public class ScalaTuple extends ScalaType {
    private final List<ScalaType> parameters = new ArrayList<>();

    public static ScalaTuple parse(Element signatureElem, String signature) {
        if (!signature.startsWith("(") || !signature.endsWith(")")) {
            return null;
        }

        ScalaTuple result = new ScalaTuple();
        List<String> params = ScalaParser.typeSplit(signature.substring(1, signature.length() - 1), ",");
        for (String paramString : params) {
            ScalaType param = ScalaType.parse(signatureElem, paramString);
            if (param == null) {
                return null;
            }

            result.parameters.add(param);
        }

        return result;
    }

    public DataType buildType(ScalaParser parser, QualifiedName className, Map<String, TypeVariable> typeVariables) {
        List<Type> params = new ArrayList<>();
        for (ScalaType param : parameters) {
            params.add(param.buildType(parser, className, typeVariables));
        }

        QualifiedName tupleName = new QualifiedName("scala", "Tuple" + String.valueOf(parameters.size()));
        return new DataType(parser.getClasses().get(tupleName), params);
    }
}
