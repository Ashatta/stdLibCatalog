package org.jetbrains.stdLibCatalog.domain;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TypeAlias extends TypeConstructor {
    private Type aliasedType;
    private final List<TypeVariable> parameters = new ArrayList<>();
    private final String definition;

    public TypeAlias(String name, Language lang, String documentation, URL docLink, String definition) {
        super(name, lang, documentation, docLink);
        this.definition = definition;
    }

    public Type getAliasedType() {
        return aliasedType;
    }

    public void setAliasedType(Type aliasedType) {
        this.aliasedType = aliasedType;
    }

    public List<TypeVariable> getParameters() {
        return parameters;
    }

    public void addParameter(TypeVariable parameter) {
        parameters.add(parameter);
    }

    public String toString() {
        String result = "[Alias]\n" + definition + "\n" + super.toString() + "\nparameters {";
        for (TypeVariable param : parameters) {
            result += "\n\t" + param.toString();
        }
        result += "\n}\nAliased type = " + aliasedType.toString();

        return result;
    }
}
