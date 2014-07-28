package org.jetbrains.stdLibCatalog.domain;

import java.util.ArrayList;
import java.util.List;

/** Base class for Classes and Interfaces */
public abstract class TypeEntity extends TypedEntity {
    protected List<FunctionEntity> functions;
    protected List<TypeEntity> derived;
    protected List<TypeEntity> base;
    protected PackageEntity containingPackage;
    protected List<TypeLink> links;

    public TypeEntity(String id, String name, String lang, String documentation, List<FunctionEntity> functions
            , List<TypeEntity> derived, List<TypeEntity> base, List<TypedEntity> parameters
            , PackageEntity containingPackage, String docLink) {
        super(id, name, lang, documentation, docLink, parameters);
        this.functions = functions;
        this.derived = derived;
        this.base = base;
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

    public void setContainingPackage(PackageEntity containingPackage) {
        this.containingPackage = containingPackage;
    }

    public List<TypeLink> getLinks() {
        return links;
    }

    public void addDerived(TypeEntity d) {
        derived.add(d);
    }

    public void addBase(TypeEntity b) {
        base.add(b);
    }
}
