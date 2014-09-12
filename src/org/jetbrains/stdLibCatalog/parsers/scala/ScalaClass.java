package org.jetbrains.stdLibCatalog.parsers.scala;

import org.jetbrains.stdLibCatalog.domain.*;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.QualifiedName;

public class ScalaClass extends ScalaType {
    private String packageName = "";
    private String name = "";
    private final List<ScalaType> parameters = new ArrayList<>();

    public static ScalaClass parse(Element signatureElem, String signature) {
        signature = ScalaParser.typeSplit(signature, "with").get(0);
        signature = ScalaParser.typeSplit(signature, "@").get(0);
        ScalaClass result = new ScalaClass();

        if (signature.endsWith("*")) {
            result.name = "*";
            result.packageName = ScalaParser.OTHER_PACKAGE;
            ScalaType param = ScalaType.parse(signatureElem, signature.substring(0, signature.length() - 1));
            result.parameters.add(param);
            return (param == null ? null : result);
        }

        String fullName = signature.split("\\[")[0];
        String[] nameParts = fullName.split("\\.");
        result.name = nameParts[nameParts.length - 1];
        result.packageName = ScalaParser.getPackageName(signatureElem, fullName);
        if (result.packageName == null) {
            return null;
        }

        if (!fullName.equals(signature)) {
            List<String> params = ScalaParser.typeSplit(signature.substring(signature.indexOf('[') + 1,
                    signature.length() - 1), ",");
            for (String paramString : params) {
                ScalaType param = ScalaType.parse(signatureElem, paramString);
                if (param == null) {
                    return null;
                }

                result.parameters.add(param);
            }
        }

        return result;
    }

    public Classifier getClassifier(ScalaParser parser) {
        for (QualifiedName typeName : getQualifiedNames()) {
            if (parser.getClasses().containsKey(typeName)) {
                return parser.getClasses().get(typeName);
            }
        }

        return null;
    }

    public DataType buildType(ScalaParser parser, QualifiedName className, Map<String, TypeVariable> typeVariables) {
        List<Type> params = new ArrayList<>();
        for (ScalaType param : parameters) {
            params.add(param.buildType(parser, className, typeVariables));
        }

        for (QualifiedName typeName : getQualifiedNames()) {
            if (parser.getClasses().containsKey(typeName)) {
                return new DataType(parser.getClasses().get(typeName), params);
            } else if (parser.getAliases().containsKey(typeName)) {
                return new DataType(parser.getAliases().get(typeName), params);
            }
        }

        throw new NullPointerException();
    }

    private List<QualifiedName> getQualifiedNames() {
        List<QualifiedName> result = new ArrayList<>();
        String[] parts = packageName.split("\\.");
        int i = 0;
        while (i < parts.length && Character.isLowerCase(parts[i].charAt(0))) {
            ++i;
        }

        for (int j = 0; j < i; ++j) {
            String packName = "";
            for (int k = 0; k <= j; ++k) {
                packName += parts[k] + ".";
            }
            packName = packName.substring(0, packName.length() - 1);

            for (String fullName : getNames(Arrays.asList(parts).subList(j + 1, parts.length))) {
                fullName = symbolNames(fullName);
                result.add(new QualifiedName(packName, fullName));
            }
        }

        return result;
    }

    private List<String> getNames(List<String> nameParts) {
        List<String> result = new ArrayList<>();
        if (nameParts.isEmpty()) {
            result.add(name);
            result.add(name + "$");
        } else {
            for (String start : getNames(nameParts.subList(1, nameParts.size()))) {
                result.add(nameParts.get(0) + "$" + start);
                result.add(nameParts.get(0) + "$$" + start);
            }
        }

        return result;
    }
}
