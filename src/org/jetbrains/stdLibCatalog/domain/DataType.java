package org.jetbrains.stdLibCatalog.domain;

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
        PackageEntity packageEntity = typeConstructor.getContainingPackage();
        String result = (packageEntity == null ? "null" : packageEntity.getName()) + "::" + typeConstructor.getName();
        if (!arguments.isEmpty()) {
            result += " {";
            for (Type arg : arguments) {
                result += "\n" + arg.toString();
            }
            result += "\n}";
        }

        return result;
    }
}
