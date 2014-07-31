package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.*;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

class HaskellConcreteType extends HaskellType {
    private String name;
    private List<HaskellType> parameters;

    HaskellConcreteType() {
        parameters = new ArrayList<>();
    }

    public static HaskellConcreteType parse(String signature, Map<String, HaskellParser.ParameterDescription> parameters) {
        if (signature.startsWith("(") && signature.endsWith(")")) {
            signature = signature.substring(1, signature.length() - 1);
        }

        if (!Character.isUpperCase(signature.charAt(0))) {
            return null;
        }

        HaskellConcreteType concrete = new HaskellConcreteType();
        List<String> params = typeSplit(signature, "");
        Iterator<String> it = params.iterator();
        concrete.name = it.next();
        while (it.hasNext()) {
            String par = it.next();
            HaskellType param = HaskellType.parse(par, parameters);
            if (param == null) {
                return null;
            }
            concrete.parameters.add(param);
        }

        return concrete;
    }

    public DataType buildType(HaskellParser parser, HaskellParser.QualifiedName entity, boolean isType) {
        String pattern = "^" + (isType ? "type\\s+" : "") + Pattern.quote(entity.getValue()) + "\\s";
        Element def = parser.shortDefinitions.get(entity.getKey()).getElementsMatchingText(pattern).last();

        Elements typeDef = def.getElementsMatchingOwnText("^" + name + "$");
        Classifier type = null;
        if (!typeDef.isEmpty()) {
            String packName = parser.getPackageName(typeDef.get(0).attributes().get("href"));
            type = parser.classes.get(new HaskellParser.QualifiedName(packName, name));
        }
        if (type == null) {
            type = new Classifier(name, "Haskell", "", "", new ArrayList<FunctionEntity>(), "");
        }

        List<TypeEntity> params = new ArrayList<>();
        for (HaskellType t : parameters) {
            params.add(t.buildType(parser, entity, isType));
        }

        return new DataType(type, params);
    }
}
