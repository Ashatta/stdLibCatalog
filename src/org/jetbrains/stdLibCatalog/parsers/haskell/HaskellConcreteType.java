package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.*;
import org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.QualifiedName;

class HaskellConcreteType extends HaskellType {
    private static List<String> INFIX_NAMES = Arrays.asList(":~:", "~", ":+:", ":*:", ":.:", "<=?");
    private static List<String> K_ALLOWED_TYPES = Arrays.asList("Map", "Set", "Weak");

    private String name;
    private List<HaskellType> parameters;

    HaskellConcreteType() {
        parameters = new ArrayList<>();
    }

    public static HaskellConcreteType parse(Element elem, String signature, List<HaskellConstraint> constraints) {
        if (signature.startsWith("(") && signature.endsWith(")")
                && !signature.equals("(->)") && !signature.matches("^\\(,*\\)$")) {
            signature = signature.substring(1, signature.length() - 1);
        }

        List<HaskellConstraint> localConstraints = new ArrayList<>();
        List<String> parts = typeSplit(signature, "=>");
        if (parts.size() > 1) {
            HaskellConstraint.parseConstraints(elem, parts.get(0), localConstraints);
            signature = parts.get(parts.size() - 1);
        }

        List<String> params = typeSplit(signature, "");
        HaskellConcreteType concrete = parseInfix(elem, params, localConstraints);
        if (concrete == null) {
            concrete = parsePrefix(elem, params, localConstraints);
        }

        if (concrete != null) {
            constraints.addAll(localConstraints);
        }

        return concrete;
    }

    private static HaskellConcreteType parseInfix(Element elem, List<String> params, List<HaskellConstraint> parameters) {
        for (int i = 1; i < params.size() - 1; ++i) {
            if (!INFIX_NAMES.contains(params.get(i))) {
                continue;
            }

            HaskellConcreteType concreteType = new HaskellConcreteType();
            concreteType.name = "(" + params.get(i) + ")";

            String paramString1 = StringUtil.join(params.subList(0, i), " ");
            HaskellType param1 = HaskellType.parse(elem, paramString1, parameters);
            if (param1 == null) {
                return null;
            }
            concreteType.parameters.add(param1);

            String paramString2 = StringUtil.join(params.subList(i + 1, params.size()), " ");
            HaskellType param2 = HaskellType.parse(elem, paramString2, parameters);
            if (param2 == null) {
                return null;
            }
            concreteType.parameters.add(param2);

            return concreteType;
        }

        return null;
    }

    private static HaskellConcreteType parsePrefix(Element elem, List<String> params, List<HaskellConstraint> constraints) {
        if (!Character.isUpperCase(params.get(0).charAt(0)) && params.get(0).charAt(0) != '(') {
            return null;
        }

        Iterator<String> it = params.iterator();
        HaskellConcreteType concrete = new HaskellConcreteType();
        concrete.name = it.next();
        if (it.hasNext()) {
            String firstParam = it.next();
            if (!(firstParam.charAt(0) == '*' || (firstParam.charAt(0) == 'k' && !K_ALLOWED_TYPES.contains(concrete.name))
                    || firstParam.charAt(0) == '(' && (firstParam.charAt(1) == '*' || firstParam.charAt(1) == 'k'))) {
                HaskellType param = HaskellType.parse(elem, firstParam, constraints);
                if (param == null) {
                    return null;
                }
                concrete.parameters.add(param);
            }
        }
        while (it.hasNext()) {
            String par = it.next();
            HaskellType param = HaskellType.parse(elem, par, constraints);
            if (param == null) {
                return null;
            }
            concrete.parameters.add(param);
        }

        return concrete;
    }

    public DataType buildType(HaskellParser parser, QualifiedName entity, boolean isType) {
        String pattern = "^" + (isType ? "type\\s+" : "") + Pattern.quote(entity.getValue()) + "\\s";
        Element def = parser.getShortDefinitions().get(entity.getKey()).getElementsMatchingText(pattern).last();

        Elements typeDef = def.getElementsMatchingOwnText("^" + Pattern.quote(name) + "$");
        TypeConstructor type = null;
        if (!typeDef.isEmpty()) {
            String packName = HaskellParser.getPackageName(typeDef.get(0).attr("href"));
            QualifiedName qualifiedName = new QualifiedName(packName, name);
            type = parser.getClasses().get(qualifiedName);
            if (type == null) {
                type = parser.getAliases().get(qualifiedName);
            }
        }
        if (type == null) {
            type = new Classifier(name, Language.HASKELL, "", null, new ArrayList<MemberEntity>(), "");
        }

        List<Type> params = new ArrayList<>();
        for (HaskellType t : parameters) {
            params.add(t.buildType(parser, entity, isType));
        }

        return new DataType(type, params);
    }

    public void extractVariables(List<String> variables, int paramsNumber) {
        for (HaskellType param : parameters) {
            param.extractVariables(variables, 0);
        }

        while (paramsNumber-- > parameters.size()) {
            variables.add("curriedParam");
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
