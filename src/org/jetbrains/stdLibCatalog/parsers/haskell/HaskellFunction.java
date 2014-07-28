package org.jetbrains.stdLibCatalog.parsers.haskell;

import javafx.util.Pair;
import org.jetbrains.stdLibCatalog.domain.FunctionEntity;
import org.jetbrains.stdLibCatalog.domain.Signature;
import org.jetbrains.stdLibCatalog.domain.TypedEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class HaskellFunction extends HaskellType {
    private List<HaskellType> arguments;

    HaskellFunction() {
        arguments = new ArrayList<>();
    }

    public static HaskellFunction parse(String signature, Map<String, Pair<Integer, List<Pair<String, String>>>> parameters) {
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

    private static void parseInterfaces(String description, Map<String, Pair<Integer, List<Pair<String, String>>>> parameters) {
        String[] classes = description.replaceAll("[()]", "").split(",\\s+");
        for (String cl : classes) {
            String[] def = cl.split("\\s+");
            if (!parameters.get(def[1]).getValue().contains(new Pair<String, String>("", def[0]))) {
                parameters.get(def[1]).getValue().add(new Pair<String, String>("", def[0]));
            }
        }
    }

    public List<HaskellType> getArguments() {
        return arguments;
    }

    public Signature makeSignature(HaskellParser parser, FunctionEntity function) {
        Iterator<HaskellType> it = arguments.iterator();
        List<TypedEntity> args = new ArrayList<>();
        HaskellType type = null;
        while (it.hasNext()) {
            type = it.next();
            if (!it.hasNext()) {
                break;
            }

            args.add(type.buildType(parser, function));
        }

        return new Signature(args, type.buildType(parser, function));
    }

    public FunctionEntity buildType(HaskellParser parser, FunctionEntity function) {
        Signature signature = makeSignature(parser, function);
        return new FunctionEntity("", "", "Haskell", signature, "", null, null, "", parameters(parser, function));
    }

    private List<TypedEntity> parameters(HaskellParser parser, FunctionEntity function) {
        List<TypedEntity> params = new ArrayList<>();
        addParameters(parser, function, params);
        return params;
    }

    public void addParameters(HaskellParser parser, FunctionEntity function, List<TypedEntity> params) {
        for (HaskellType type : arguments) {
            type.addParameters(parser, function, params);
        }
    }
}
