package org.jetbrains.stdLibCatalog.parsers.java;

import org.jetbrains.stdLibCatalog.domain.FunctionType;
import org.jetbrains.stdLibCatalog.domain.Type;
import org.jetbrains.stdLibCatalog.domain.TypeVariable;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.QualifiedName;

public class JavaFunction {
    private final List<JavaType> parameters = new ArrayList<>();
    private JavaType result;

    public static JavaFunction parse(Element elem, String typeString) {
        JavaFunction result = new JavaFunction();
        result.result = JavaType.parse(elem, typeString.substring(0, typeString.indexOf('(')));

        typeString = typeString.substring(typeString.indexOf('(') + 1, typeString.length() - 1);
        List<String> paramStrings = JavaParser.typeSplit(typeString, ",");
        for (String param : paramStrings) {
            result.parameters.add(JavaType.parse(elem, JavaParser.typeSplit(param, "").get(0)));
        }

        return result;
    }

    public FunctionType buildFunction(JavaParser parser, Map<String, TypeVariable> entityVars
            , Map<String, TypeVariable> parentVars, QualifiedName enclosingClass) {
        List<Type> params = new ArrayList<>();
        for (JavaType param : parameters) {
            params.add(param.buildType(parser, entityVars, parentVars, enclosingClass));
        }

        return new FunctionType(params, result.buildType(parser, entityVars, parentVars, enclosingClass));
    }

    private JavaFunction() {}
}
