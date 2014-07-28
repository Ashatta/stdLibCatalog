package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.*;
import javafx.util.Pair;
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

    public static HaskellConcreteType parse(String signature, Map<String, Pair<Integer, List<Pair<String, String>>>> parameters) {
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

    public String getName() {
        return name;
    }

    public List<HaskellType> getParameters() {
        return parameters;
    }

    public ClassEntity buildType(HaskellParser parser, FunctionEntity function) {
        Element def = parser.shortDefinitions.get(function.getContainingPackage().getName())
                .getElementsMatchingText("^" + Pattern.quote(function.getName()) + "\\s").get(0);

        Elements typeDef = def.getElementsMatchingOwnText("^" + name + "$");
        ClassEntity type = null;
        if (!typeDef.isEmpty()) {
            String packName = parser.getPackageName(typeDef.get(0).attributes().get("href"));
            if (parser.classes.get(packName).containsKey(name)) {
                type = parser.classes.get(packName).get(name).clone();
            }
        }
        if (type == null) {
            type = new ClassEntity("", name, "Haskell", "", new ArrayList<FunctionEntity>(), new ArrayList<TypeEntity>()
                    , new ArrayList<TypeEntity>(), null, new ArrayList<TypedEntity>(), ""
                    , new ArrayList<InterfaceEntity>());
        }

        List<TypedEntity> params = new ArrayList<>();
        for (HaskellType t : parameters) {
            params.add(t.buildType(parser, function));
        }

        type.setParameters(params);
        return type;
    }

    public void addParameters(HaskellParser parser, FunctionEntity function, List<TypedEntity> params) {
        for (HaskellType type : parameters) {
            type.addParameters(parser, function, params);
        }
    }
}
