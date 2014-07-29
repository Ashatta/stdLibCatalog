package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.FunctionEntity;
import org.jetbrains.stdLibCatalog.domain.FunctionType;
import org.jetbrains.stdLibCatalog.domain.TypeEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class HaskellType {
    public static HaskellType parse(String signature, Map<String, HaskellParser.ParameterDescription> parameters) {
        HaskellList list = HaskellList.parse(signature, parameters);
        if (list != null) {
            return list;
        }

        HaskellTuple tuple = HaskellTuple.parse(signature, parameters);
        if (tuple != null) {
            return tuple;
        }

        HaskellConcreteType concrete = HaskellConcreteType.parse(signature, parameters);
        if (concrete != null) {
            return concrete;
        }

        HaskellParameter param = HaskellParameter.parse(signature, parameters);
        if (param != null) {
            return param;
        }

        HaskellFunction func = HaskellFunction.parse(signature, parameters);
        if (func != null) {
            return func;
        }

        return null;
    }

    protected static List<String> typeSplit(String signature, String str) {
        List<String> result = new ArrayList<>();
        int braces = 0;
        int start = 0;
        int len = str.length();
        for (int i = 0; i < signature.length(); ++i) {
            i = skip(signature, i, len);

            if (signature.charAt(i) == '(' || signature.charAt(i) == '[') {
                braces++;
            } else if (signature.charAt(i) == ')' || signature.charAt(i) == ']') {
                braces--;
            } else if (braces == 0 && substr(signature, str, i, len)) {
                result.add(signature.substring(start, i).trim());
                i += Math.max(len - 1, 0);
                start = i + 1;
            }
        }

        String last = signature.substring(start, signature.length()).trim();
        if (!last.isEmpty()) {
            result.add(last);
        }

        return result;
    }

    private static boolean substr(String signature, String str, int i, int len) {
        if (len == 0) {
            return Character.isWhitespace(signature.charAt(i));
        } else {
            return  i + len < signature.length() && signature.substring(i, i + len).equals(str);
        }
    }

    private static int skip(String signature, int i, int len) {
        if (len != 0) {
            while (Character.isWhitespace(signature.charAt(i))) {
                ++i;
            }
        }

        return i;
    }

    public FunctionType makeSignature(HaskellParser parser, FunctionEntity function) {
        return new FunctionType(new ArrayList<TypeEntity>(), buildType(parser, function));
    }

    abstract public TypeEntity buildType(HaskellParser parser, FunctionEntity function);
}
