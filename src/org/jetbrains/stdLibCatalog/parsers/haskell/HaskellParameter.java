package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class HaskellParameter extends HaskellType {
    private String name;
    private List<HaskellType> parameters;

    HaskellParameter() {
        parameters = new ArrayList<>();
    }

    public static HaskellParameter parse(String signature, Map<String, HaskellParser.ParameterDescription> parameters) {
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

    public DataType buildType(HaskellParser parser, FunctionEntity function) {
        String packName = function.getContainingPackage().getName();

        List<TypeEntity> params = new ArrayList<>();
        for (HaskellType param : parameters) {
            params.add(param.buildType(parser, function));
        }

        return new DataType(parser.functionEndParameters.get(new HaskellParser.QualifiedName(packName, function.getName())).get(name), params);
    }
}
