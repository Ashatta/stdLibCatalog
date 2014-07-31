package org.jetbrains.stdLibCatalog.domain;

import java.util.ArrayList;
import java.util.List;

public class FunctionEntity extends Entity {
    private FunctionType signature;
    private Classifier containingType;
    private final List<TypeConstructor> parameters = new ArrayList<>();
    private final List<FunctionLink> links = new ArrayList<>();

    public FunctionEntity(String lang, String name, String documentation, String docLink) {
        super(lang, name, documentation, docLink);
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

    public List<TypeConstructor> getParameters() {
        return parameters;
    }

    public void addParameter(TypeConstructor param) {
        parameters.add(param);
    }

    public List<FunctionLink> getLinks() {
        return links;
    }
}
