package com.jetbrains.agapova.stdLibCatalog.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ashatta on 7/10/14.
 */
/** Base class for Classes and Interfaces */
public class TypeEntity extends Entity {
    protected List<FunctionEntity> functions;
    protected List<TypeEntity> derived;
    protected List<TypeEntity> base;
    protected List<TypeEntity> parameters;      // generic parameters etc.
    protected PackageEntity containingPackage;
    protected List<TypeLink> links;

    public TypeEntity(String id, String name, String lang, String documentation, List<FunctionEntity> functions
            , List<TypeEntity> derived, List<TypeEntity> base, List<TypeEntity> parameters
            , PackageEntity containingPackage, String docLink) {
        super(id, name, lang, documentation, docLink);
        this.functions = functions;
        this.derived = derived;
        this.base = base;
        this.parameters = parameters;
        this.containingPackage = containingPackage;
        this.links = new ArrayList<TypeLink>();
    }

    public List<FunctionEntity> getFunctions() {
        return functions;
    }

    public List<TypeEntity> getDerived() {
        return derived;
    }

    public List<TypeEntity> getBase() {
        return base;
    }

    public PackageEntity getContainingPackage() {
        return containingPackage;
    }

    public List<TypeLink> getLinks() {
        return links;
    }

    public List<TypeEntity> getParameters() {
        return parameters;
    }
}
