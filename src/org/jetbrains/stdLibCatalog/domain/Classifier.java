package org.jetbrains.stdLibCatalog.domain;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Classifier extends TypeConstructor {
    private final List<MemberEntity> functions;
    private final List<Classifier> derived = new ArrayList<>();
    private final List<Classifier> base = new ArrayList<>();
    private final List<TypeVariable> parameters = new ArrayList<>();
    private final String definition;

    public Classifier(String name, Language lang, String documentation, URL docLink, List<MemberEntity> functions,
            String definition) {
        super(name, lang, documentation, docLink);
        this.functions = functions;
        this.definition = definition;
    }

    public List<MemberEntity> getFunctions() {
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

    public List<TypeVariable> getParameters() {
        return parameters;
    }

    public void addParameter(TypeVariable param) {
        parameters.add(param);
    }
}
