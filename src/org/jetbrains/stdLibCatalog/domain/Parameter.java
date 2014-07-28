package org.jetbrains.stdLibCatalog.domain;

import java.util.List;

public class Parameter extends TypedEntity {
    private int parameterId;
    private List<InterfaceEntity> interfaces;

    public Parameter(int parameterId, List<InterfaceEntity> interfaces) {
        super("", "", "", "", "", null);
        this.parameterId = parameterId;
        this.interfaces = interfaces;
    }

    public int getParameterId() {
        return parameterId;
    }

    public void setParameterId(int parameterId) {
        this.parameterId = parameterId;
    }

    public List<InterfaceEntity> getInterfaces() {
        return interfaces;
    }

    public Parameter clone() {
        return new Parameter(parameterId, interfaces);
    }
}
