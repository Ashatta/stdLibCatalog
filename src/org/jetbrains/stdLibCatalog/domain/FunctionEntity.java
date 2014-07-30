package org.jetbrains.stdLibCatalog.domain;

import java.util.ArrayList;
import java.util.List;

public class FunctionEntity extends Entity {
    private FunctionType signature;
    private Classifier containingType;
    private PackageEntity containingPackage;
    private final List<TypeConstructor> parameters;
    private final List<FunctionLink> links;

    public FunctionEntity(String lang, String name, String documentation, String docLink) {
        super(lang, name, documentation, docLink);
        this.signature = null;
        this.containingType = null;
        this.containingPackage = null;
        this.parameters = new ArrayList<>();
        this.links = new ArrayList<>();
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

    public PackageEntity getContainingPackage() {
        return containingPackage;
    }

    public void setContainingPackage(PackageEntity containingPackage) {
        this.containingPackage = containingPackage;
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
