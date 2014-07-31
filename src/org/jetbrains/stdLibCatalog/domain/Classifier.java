package org.jetbrains.stdLibCatalog.domain;

import java.util.ArrayList;
import java.util.List;

public class Classifier extends TypeConstructor {
    private final List<FunctionEntity> functions;
    private final List<Classifier> derived = new ArrayList<>();
    private final List<Classifier> base = new ArrayList<>();
    private final List<TypeConstructor> parameters = new ArrayList<>();
    private final List<TypeLink> links = new ArrayList<>();

    public Classifier(String name, String lang, String documentation, String docLink, List<FunctionEntity> functions) {
        super(name, lang, documentation, docLink);
        this.functions = functions;
    }

    public List<FunctionEntity> getFunctions() {
        return functions;
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
