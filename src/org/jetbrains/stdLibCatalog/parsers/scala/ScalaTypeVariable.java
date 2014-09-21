package org.jetbrains.stdLibCatalog.parsers.scala;

import org.jetbrains.stdLibCatalog.domain.*;
import org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils;
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

    public Classifier getClassifier(ScalaParser parser) {
        return null;
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
            String packageName = getPackageName();
            constructor = specialCase(parser, packageName);
            if (constructor == null) {
                QualifiedName fullName = getFullName(packageName.substring(0, packageName.length() - 1));
                constructor = (parser.JAVA_PARSER.getClasses().containsKey(fullName)
                        ? parser.JAVA_PARSER.getClasses().get(fullName)
                        : getScalaClass(parser, fullName));
            }
        }

        return new DataType(constructor, params);
    }

    public String getPackageName() {
        String packageName = "";
        if (!qualifiedName.isEmpty()) {
            for (String part : qualifiedName.split("\\.")) {
                if (!Character.isLowerCase(part.charAt(0))) {
                    break;
                }
                packageName += part + ".";
            }
        }
        return packageName;
    }

    private TypeConstructor specialCase(ScalaParser parser, String packageName) {
        if (Arrays.asList("CC", "ConcurrentMap").contains(name) || name.endsWith(".type")) {
            return new TypeVariable(name, Language.SCALA);
        }

        if (packageName.isEmpty()) {
            if (name.startsWith("Double(") || name.startsWith("Int(") || name.startsWith("Char(")
                    || name.startsWith("Float(") || name.startsWith("Byte(") || name.startsWith("Long(")
                    || name.startsWith("Short(")) {
                name = name.substring(0, name.indexOf('('));
                return parser.getClasses().get(new QualifiedName("scala", name));
            }
            if (name.equals("String") || name.startsWith("String(")) {
                return parser.getAliases().get(new QualifiedName("scala", "Predef$$String"));
            }
            if (name.equals("ArrayBuffer")) {
                return parser.getClasses().get(new QualifiedName("scala.collection.mutable", "ArrayBuffer"));
            }
            if (name.equals("Growable") || name.equals("Shrinkable")) {
                return parser.getClasses().get(new QualifiedName("scala.collection.generic", name));
            }
        }

        return null;
    }

    public QualifiedName getFullName(String packageName) {
        if (packageName.equals("immutable")) {
            packageName = "scala.collection.immutable";
        }

        String[] nameParts = name.split("\\.");
        name = nameParts[nameParts.length - 1];
        if (packageName.equals("java.lang") && (name.equals("UncaughtExceptionHandler") || name.equals("State"))) {
            name = "Thread." + name;
        }
        if (packageName.equals("java.util.concurrent") && name.startsWith("TimeUnit(")) {
            name = "TimeUnit";
        }
        if (packageName.equals("java.util.jar") && name.equals("Name")) {
            name = "Attributes.Name";
        }

        return new QualifiedName(packageName, name);
    }

    public TypeConstructor getScalaClass(ScalaParser parser, QualifiedName className) {
        if (!parser.getClasses().containsKey(className)) {
            Classifier classifier = new Classifier(name,
                    className.getKey().startsWith("scala") ? Language.SCALA : Language.JAVA, "", null,
                    new ArrayList<MemberEntity>(), "");
            parser.getClasses().put(className, classifier);
            classifier.setContainingPackage(parser.getPackages().get(className.getKey()));
            parser.getPackages().get(className.getKey()).addContainedClass(classifier);
        }

        return parser.getClasses().get(className);
    }
}
