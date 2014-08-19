package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.*;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class HaskellParameter extends HaskellType {
    private String name;
    private List<HaskellType> parameters;

    HaskellParameter() {
        parameters = new ArrayList<>();
    }

    public static HaskellParameter parse(Element elem, String signature, List<HaskellConstraint> constraints) {
        if (signature.startsWith("(") && signature.endsWith(")")) {
            signature = signature.substring(1, signature.length() - 1);
        }
        signature = typeSplit(signature, "::").get(0);

        List<HaskellConstraint> localConstraints = new ArrayList<>();
        List<String> parts = typeSplit(signature, "=>");
        if (parts.size() > 1) {
            HaskellConstraint.parseConstraints(elem, parts.get(0), localConstraints);
            signature = parts.get(parts.size() - 1);
        }

        if (!Character.isLowerCase(signature.charAt(0))) {
            return null;
        }

        HaskellParameter parameter = new HaskellParameter();
        List<String> params = typeSplit(signature, "");
        Iterator<String> it = params.iterator();
        parameter.name = it.next();
        while (it.hasNext()) {
            String par = it.next();
            HaskellType param = HaskellType.parse(elem, par, localConstraints);
            if (param == null) {
                return null;
            }
            parameter.parameters.add(param);
        }

        constraints.addAll(localConstraints);
        return parameter;
    }

    public DataType buildType(HaskellParser parser, HaskellParser.QualifiedName entity, boolean isType) {
        List<Type> params = new ArrayList<>();
        for (HaskellType param : parameters) {
            params.add(param.buildType(parser, entity, isType));
        }

        for (TypeVariable param : parser.getEntityParams().get(entity)) {
            if (param.getName().equals(name)) {
                return new DataType(param, params);
            }
        }

        return null;
    }

    public void extractVariables(List<String> variables, int paramsNumber) {
        if (!variables.contains(name)) {
            variables.add(name);
        }
        for (HaskellType param : parameters) {
            param.extractVariables(variables, 0);
        }
    }

    public String getName() {
        return name;
    }

    protected String classifierName() {
        String result = name + "<";
        for (HaskellType param : parameters) {
            result += param.classifierName() + ",";
        }
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1);
        }
        return result + ">";
    }
}

