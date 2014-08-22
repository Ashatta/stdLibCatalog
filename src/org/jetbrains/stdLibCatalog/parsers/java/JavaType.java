package org.jetbrains.stdLibCatalog.parsers.java;

import javafx.util.Pair;
import org.jetbrains.stdLibCatalog.domain.*;
import org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.*;

public class JavaType {
    private String packageName = "";
    private String name = "";
    private final List<JavaType> parameters = new ArrayList<>();

    public static JavaType parse(Element elem, String typeString) {
        JavaType type = new JavaType();

        if (typeString.endsWith("[]") || typeString.endsWith("...")) {
            type.name = typeString.endsWith("[]") ? "[]" : "...";
            type.packageName = JavaParser.OTHER_PACKAGE;
            type.parameters.add(JavaType.parse(elem,
                    typeString.substring(0, typeString.length() - (typeString.endsWith("[]") ? 2 : 3))));
            return type;
        }

        if (typeString.startsWith("?")) {
            return JavaWildcard.parse(elem, typeString);
        }

        type.name = typeString.split("<")[0];
        type.packageName = JavaParser.PRIMITIVE_TYPES.contains(type.name)
                ? JavaParser.OTHER_PACKAGE : JavaParser.getPackageName(elem, type.name);

        String paramsString = typeString.replaceFirst(Pattern.quote(type.name), "");
        if (!paramsString.isEmpty()) {
            paramsString = paramsString.substring(1, paramsString.length() - 1);
            List<String> params = JavaParser.typeSplit(paramsString, ",");
            for (String param : params) {
                type.parameters.add(JavaType.parse(elem, param));
            }
        }

        return type;
    }

    public DataType buildType(JavaParser parser, Map<String, TypeVariable> entityVars
            , Map<String, TypeVariable> parentVars, QualifiedName enclosingClass) {
        List<Type> params = new ArrayList<>();
        for (JavaType param : parameters) {
            params.add(param.buildType(parser, entityVars, parentVars, enclosingClass));
        }

        if (entityVars.containsKey(name)) {
            return new DataType(entityVars.get(name), params);
        }
        if (parentVars.containsKey(name)) {
            return  new DataType(parentVars.get(name), params);
        }

        if (packageName.equals("null")) {
            String[] parts = enclosingClass.getValue().split("\\.");
            if (parts[parts.length - 1].equals(name)) {  // constructor
                return new DataType(parser.getClasses().get(enclosingClass), params);
            } else if (Character.isLowerCase(name.charAt(0))) {  // classes not present in docs
                return new DataType(new Classifier(name, Language.JAVA, "", null, new ArrayList<MemberEntity>(), name), params);
            } else {  // undeclared type parameter
                TypeVariable var = new TypeVariable(name, Language.JAVA);
                entityVars.put(name, var);
                return new DataType(var, params);
            }
        }

        Classifier classifier = parser.getClasses().get(new QualifiedName(packageName, name));
        if (classifier != null) {
            return new DataType(classifier, params);
        } else {  // type variables from outer class in inner class (JList.AccessibleJList.AcessibleJListChild)
            TypeVariable var = new TypeVariable(name, Language.JAVA);
            entityVars.put(name, var);
            return new DataType(var, params);
        }
    }

    public FunctionType buildAsFunction(JavaParser parser, Map<String, TypeVariable> entityVars
            , Map<String, TypeVariable> parentVars, QualifiedName enclosingClass) {
        return new FunctionType(new ArrayList<Type>(), buildType(parser, entityVars, parentVars, enclosingClass));
    }

    public void extractVariables(Map<String, TypeVariable> entityVars
            , Map<String, TypeVariable> parentVars, List<TypeVariable> result) {
        if (entityVars.containsKey(name) && !result.contains(entityVars.get(name))) {
            result.add(entityVars.get(name));
        }
        if (parentVars.containsKey(name) && !result.contains(parentVars.get(name))) {
            result.add(parentVars.get(name));
        }

        for (JavaType param : parameters) {
            param.extractVariables(entityVars, parentVars, result);
        }
    }

    public void extractOtherEntities(JavaParser parser, Map<String, TypeVariable> entityVars
            , Map<String, TypeVariable> parentVars, List<Entity> result) {
        QualifiedName qualifiedName = new QualifiedName(packageName, name);
        Entity entity = parser.getClasses().get(qualifiedName);
        if (entity != null && !result.contains(entity)) {
            result.add(entity);
        }

        for (JavaType param : parameters) {
            param.extractOtherEntities(parser, entityVars, parentVars, result);
        }
    }

    public String buildString() {
        String result = name;
        if (!parameters.isEmpty()) {
            result += "<";
            for (JavaType param : parameters) {
                result += param.buildString() + ",";
            }
            result = result.substring(0, result.length() - 1) + ">";
        }

        return result;
    }

    protected JavaType() {}
}
