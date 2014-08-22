package org.jetbrains.stdLibCatalog.parsers.utils;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class ParserUtils {
    public static class QualifiedName extends Pair<String, String> {
        public QualifiedName(String packageName, String entityName) {
            super(packageName, entityName);
        }
    }

    public static List<String> typeSplit(String signature, String str, List<Character> openingBraces,
            List<Character> closingBraces) {
        List<String> result = new ArrayList<>();
        int braces = 0;
        int start = 0;
        int len = str.length();
        for (int i = 0; i < signature.length(); ++i) {
            i = skip(signature, i, len);

            if (openingBraces.contains(signature.charAt(i))) {
                braces++;
            } else if (closingBraces.contains(signature.charAt(i))) {
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
