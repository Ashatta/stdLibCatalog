package org.jetbrains.stdLibCatalog.domain;

import org.jsoup.helper.StringUtil;

import java.util.Arrays;
import java.util.List;

public class FunctionType extends Type {
    private final List<Type> parameters;
    private final Type result;

    public FunctionType(List<Type> parameters, Type result) {
        this.parameters = parameters;
        this.result = result;
    }

    public List<Type> getParameters() {
        return parameters;
    }

    public Type getResult() {
        return result;
    }

    public String toString() {
        String result = "<function> {\n\t[parameters]";

        for (Type param : parameters) {
            result += "\n\t" + StringUtil.join(Arrays.asList(param.toString().split("\n")), "\n\t");
        }

        result += "\n\n\t[result]\n\t" + StringUtil.join(Arrays.asList(this.result.toString().split("\n")), "\n\t") + "\n}";
        return result;
    }
}
