package com.jetbrains.agapova.stdLibCatalog.parsers.haskellParser;

import com.jetbrains.agapova.stdLibCatalog.domain.FunctionEntity;
import com.jetbrains.agapova.stdLibCatalog.domain.InterfaceEntity;
import com.jetbrains.agapova.stdLibCatalog.domain.Parameter;
import com.jetbrains.agapova.stdLibCatalog.domain.TypedEntity;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
* Created by ashatta on 7/18/14.
*/
class HaskellParameter extends HaskellType {
    private String name;
    private List<HaskellType> parameters;

    HaskellParameter() {
        parameters = new ArrayList<>();
    }

    public static HaskellParameter parse(String signature, Map<String, Pair<Integer, List<Pair<String, String>>>> parameters) {
        if (signature.startsWith("(") && signature.endsWith(")")) {
            signature = signature.substring(1, signature.length() - 1);
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
            HaskellType param = HaskellType.parse(par, parameters);
            if (param == null) {
                return null;
            }
            parameter.parameters.add(param);
        }

        return parameter;
    }

    public List<HaskellType> getParameters() {
        return parameters;
    }

    public Parameter buildType(HaskellParser parser, FunctionEntity function) {
        String packName = function.getContainingPackage().getName();
        Pair<Integer, List<Pair<String, String>>> desc
                = parser.functionParameters.get(packName).get(function.getName()).get(name);
        List<InterfaceEntity> interfaces = new ArrayList<>();
        for (Pair<String, String> interf : desc.getValue()) {
            if (parser.interfaces.containsKey(interf.getKey())) {
                interfaces.add(parser.interfaces.get(interf.getKey()).get(interf.getValue()));
            }
        }

        if (!parser.functionEndParameters.get(packName).get(function.getName()).containsKey(name)) {
            parser.functionEndParameters.get(packName).get(function.getName()).put(
                    name
                    , new Parameter(desc.getKey(), interfaces));
        }
        return parser.functionEndParameters.get(packName).get(function.getName()).get(name);
    }

    public void addParameters(HaskellParser parser, FunctionEntity function, List<TypedEntity> params) {
        String packName = function.getContainingPackage().getName();
        if (!params.contains(parser.functionEndParameters.get(packName).get(function.getName()).get(name))) {
            params.add(parser.functionEndParameters.get(packName).get(function.getName()).get(name));
        }
    }
}
