package org.jetbrains.stdLibCatalog.domain;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MemberEntity extends Entity {
    private FunctionType signature;
    private Classifier containingType;
    private final List<TypeVariable> parameters = new ArrayList<>();
    private final String definition;

    public MemberEntity(String name, Language lang, String documentation, URL docLink, String definition) {
        super(name, lang, documentation, docLink);
        this.definition = definition;
        this.signature = null;
        this.containingType = null;
    }

    public FunctionType getSignature() {
        return signature;
    }

    public Classifier getContainingType() {
        return containingType;
    }

    public void setContainingType(Classifier containingType) {
        this.containingType = containingType;
    }

    public void setSignature(FunctionType signature) {
        this.signature = signature;
    }

    public List<TypeVariable> getParameters() {
        return parameters;
    }

    public void addParameter(TypeVariable param) {
        parameters.add(param);
    }

    public String toString() {
        String result = "[Function]\n" + definition + "\n" + super.toString()
                + "\nContaining type: " + (containingType != null ? containingType.getName() : "null")
                + "\nparameters {";

        for (TypeVariable param : parameters) {
            result += "\n\t" + param.toString();
        }
        result += "\n}\nSignature = " + signature.toString();

        return result;
    }
}
