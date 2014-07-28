package org.jetbrains.stdLibCatalog.domain;

import java.util.List;

/**
 * Represents entity that can be a function parameter
 */
public abstract class TypedEntity extends Entity {
    protected List<TypedEntity> parameters;

    public TypedEntity(String id, String name, String lang, String documentation, String docLink
            , List<TypedEntity> parameters) {
        super(id, name, lang, documentation, docLink);
        this.parameters = parameters;
    }

    public List<TypedEntity> getParameters() {
        return parameters;
    }

    public void setParameters(List<TypedEntity> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(Parameter param) {
        parameters.add(param);
    }
}
