package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.FunctionEntity;
import org.jetbrains.stdLibCatalog.domain.FunctionType;
import org.jetbrains.stdLibCatalog.domain.TypeEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class HaskellFunction extends HaskellType {
    private List<HaskellType> arguments;

    HaskellFunction() {
        arguments = new ArrayList<>();
    }

    public static HaskellFunction parse(String signature, Map<String, HaskellParser.ParameterDescription> parameters) {
        if (!signature.startsWith("(") || !signature.endsWith(")")) {
            return null;
        }

        signature = signature.substring(1, signature.length() - 1);

        HaskellFunction func = new HaskellFunction();

        List<String> types = typeSplit(signature, "->");

        for (String arg : types) {
            List<String> parts = typeSplit(arg, "=>");
            if (parts.size() > 1) {
                parseInterfaces(parts.get(0), parameters);
            }

            HaskellType a = HaskellType.parse(parts.get(parts.size() - 1), parameters);
            if (a == null) {
                return null;
            }
            func.arguments.add(a);
        }

        return func;
    }

    private static void parseInterfaces(String description, Map<String, HaskellParser.ParameterDescription> parameters) {
        String[] classes = description.replaceAll("[()]", "").split(",\\s+");
        for (String cl : classes) {
            String[] def = cl.split("\\s+");
            HaskellParser.QualifiedName interfaceName = new HaskellParser.QualifiedName("", def[0]);
            if (!parameters.get(def[1]).getValue().contains(interfaceName)) {
                parameters.get(def[1]).getValue().add(interfaceName);
            }
        }
    }

    public FunctionType makeSignature(HaskellParser parser, FunctionEntity function) {
        Iterator<HaskellType> it = arguments.iterator();
        List<TypeEntity> args = new ArrayList<>();
        HaskellType type = null;
        while (it.hasNext()) {
            type = it.next();
            if (!it.hasNext()) {
                break;
            }

            args.add(type.buildType(parser, function));
        }

        assert type != null;
        return new FunctionType(args, type.buildType(parser, function));
    }

    public FunctionType buildType(HaskellParser parser, FunctionEntity function) {
        return makeSignature(parser, function);
    }
}
