package com.jetbrains.agapova.stdLibCatalog.domain;

import java.util.List;

/**
 * Created by ashatta on 7/15/14.
 */
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
}
