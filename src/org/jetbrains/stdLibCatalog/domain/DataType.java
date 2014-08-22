package org.jetbrains.stdLibCatalog.domain;

import org.jsoup.helper.StringUtil;

import java.util.Arrays;
import java.util.List;

public class DataType extends Type {
    private final TypeConstructor typeConstructor;
    private final List<Type> arguments;

    public DataType(TypeConstructor typeConstructor, List<Type> arguments) {
        this.typeConstructor = typeConstructor;
        this.arguments = arguments;
    }

    //todo dphJB not used!
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    public List<Type> getArguments() {
        return arguments;
    }

    public String toString() {
        if (typeConstructor instanceof Wildcard) {
            return typeConstructor.toString();
        }

        PackageEntity packageEntity = typeConstructor.getContainingPackage();
        String result = (packageEntity == null ? "null" : packageEntity.getName()) + "::" + typeConstructor.getName();
        if (!arguments.isEmpty()) {
            result += " {";
            for (Type arg : arguments) {
                result += "\n\t" + StringUtil.join(Arrays.asList(arg.toString().split("\n")), "\n\t");
            }
            result += "\n}";
        }

        return result;
    }
}
