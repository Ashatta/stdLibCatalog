package org.jetbrains.stdLibCatalog.domain;

import java.util.ArrayList;
import java.util.List;

public class Classifier extends TypeConstructor {
    private final List<FunctionEntity> functions;
    private final List<Classifier> derived;
    private final List<Classifier> base;
    private PackageEntity containingPackage;
    private final List<TypeConstructor> parameters;
    private final List<TypeLink> links;

    public Classifier(String name, String lang, String documentation, List<FunctionEntity> functions
            , String docLink) {
        super(name, lang, documentation, docLink);
        this.functions = functions;
        this.derived = new ArrayList<>();
        this.base = new ArrayList<>();
        this.containingPackage = null;
        this.parameters = new ArrayList<>();
        this.links = new ArrayList<>();
    }

    public List<FunctionEntity> getFunctions() {
        return functions;
    }

    public PackageEntity getContainingPackage() {
        return containingPackage;
    }

    public void setContainingPackage(PackageEntity containingPackage) {
        this.containingPackage = containingPackage;
    }

    public void addDerived(Classifier d) {
        if (!derived.contains(d)) {
            derived.add(d);
        }
    }

    public void addBase(Classifier b) {
        if (!base.contains(b)) {
            base.add(b);
        }
    }

    public List<TypeConstructor> getParameters() {
        return parameters;
    }

    public void addParameter(TypeConstructor param) {
        parameters.add(param);
    }

    public List<TypeLink> getLinks() {
        return links;
    }
}
