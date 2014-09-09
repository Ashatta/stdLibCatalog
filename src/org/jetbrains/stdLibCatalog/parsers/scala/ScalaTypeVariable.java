package org.jetbrains.stdLibCatalog.parsers.scala;

import com.sun.org.omg.CORBA.ExcDescriptionSeqHelper;
import org.jetbrains.stdLibCatalog.domain.*;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.QualifiedName;

public class ScalaTypeVariable extends ScalaType {
    private String name;
    private final List<ScalaType> parameters = new ArrayList<>();
    private String qualifiedName;

    public static ScalaTypeVariable parse(Element signatureElem, String signature) {
        signature = ScalaParser.typeSplit(signature, "with").get(0);
        signature = ScalaParser.typeSplit(signature, "@").get(0);
        ScalaTypeVariable result = new ScalaTypeVariable();

        if (signature.startsWith("String(")) {
            result.name = "String";
            result.qualifiedName = "String";
            return result;
        }

        result.name = signature.split("\\[")[0];
        if (!result.name.equals(signature)) {
            List<String> params = ScalaParser.typeSplit(signature.substring(result.name.length() + 1,
                    signature.length() - 1), ",");
            for (String paramString : params) {
                ScalaType param = ScalaType.parse(signatureElem, paramString);
                if (param == null) {
                    return null;
                }

                result.parameters.add(param);
            }
        }

        if (result.name.contains(" ") || result.name.contains(",")) {
            return null;
        }

        result.name = symbolNames(result.name);
        Elements linkElems = signatureElem.getElementsMatchingOwnText("^" + Pattern.quote(result.name) + "$");
        result.qualifiedName = !linkElems.isEmpty() ? linkElems.get(linkElems.size() - 1).attr("name") : result.name;

        return result;
    }

    public DataType buildType(ScalaParser parser, QualifiedName className, Map<String, TypeVariable> typeVariables) {
        List<Type> params = new ArrayList<>();
        for (ScalaType param : parameters) {
            params.add(param.buildType(parser, className, typeVariables));
        }

        TypeConstructor constructor;
        if (className.getValue().endsWith(name)) {
            constructor = parser.getClasses().get(className);
        } else if (typeVariables.containsKey(name)) {
            constructor = typeVariables.get(name);
        } else {
            String packageName = "";
            if (!qualifiedName.isEmpty()) {
                for (String part : qualifiedName.split("\\.")) {
                    if (!Character.isLowerCase(part.charAt(0))) {
                        break;
                    }
                    packageName += part + ".";
                }
            }

            if (packageName.isEmpty()) {
                if (Arrays.asList("CC", "ConcurrentMap").contains(name) || name.endsWith(".type")) {
                    return new DataType(new TypeVariable(name, Language.SCALA), params);
                }
                if (name.startsWith("Double(") || name.startsWith("Int(") || name.startsWith("Char(")
                        || name.startsWith("Float(") || name.startsWith("Byte(") || name.startsWith("Long(")
                        || name.startsWith("Short(")) {
                    name = name.substring(0, name.indexOf('('));
                    return new DataType(parser.getClasses().get(new QualifiedName("scala", name)), params);
                }
                if (name.equals("String") || name.startsWith("String(")) {
                    return new DataType(parser.getAliases().get(new QualifiedName("scala", "Predef$$String")), params);
                }
                if (name.equals("ArrayBuffer")) {
                    return new DataType(parser.getClasses().get(new QualifiedName("scala.collection.mutable", "ArrayBuffer")), params);
                }
                if (name.equals("Growable") || name.equals("Shrinkable")) {
                    return new DataType(parser.getClasses().get(new QualifiedName("scala.collection.generic", name)), params);
                }
            }

            packageName = packageName.substring(0, packageName.length() - 1);

            String[] nameParts = name.split("\\.");
            name = nameParts[nameParts.length - 1];
            QualifiedName fullName = new QualifiedName(packageName, name);
            if (!parser.getClasses().containsKey(fullName)) {
                parser.getClasses().put(fullName, new Classifier(name,
                        packageName.startsWith("scala") ? Language.SCALA : Language.JAVA, "", null,
                        new ArrayList<MemberEntity>(), ""));
            }

            constructor = parser.getClasses().get(fullName);
        }

        return new DataType(constructor, params);
    }
}
