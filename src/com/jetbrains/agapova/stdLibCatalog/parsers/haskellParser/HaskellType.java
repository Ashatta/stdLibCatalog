package com.jetbrains.agapova.stdLibCatalog.parsers.haskellParser;

import java.util.ArrayList;
import java.util.List;

/**
* Created by ashatta on 7/18/14.
*/
class HaskellType {
    public static HaskellType parse(String signature) {
        HaskellFunction func = HaskellFunction.parse(signature);
        if (func != null) {
            return func;
        }

        HaskellList list = HaskellList.parse(signature);
        if (list != null) {
            return list;
        }

        HaskellTuple tuple = HaskellTuple.parse(signature);
        if (tuple != null) {
            return tuple;
        }

        HaskellConcreteType concrete = HaskellConcreteType.parse(signature);
        if (concrete != null) {
            return concrete;
        }

        HaskellParameter param = HaskellParameter.parse(signature);
        if (param != null) {
            return param;
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

            if (signature.charAt(i) == '(') {
                braces++;
            } else if (signature.charAt(i) == ')') {
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
}
