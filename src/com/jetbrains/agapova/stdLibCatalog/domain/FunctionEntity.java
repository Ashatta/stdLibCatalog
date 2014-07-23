package com.jetbrains.agapova.stdLibCatalog.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ashatta on 7/10/14.
 */
public class FunctionEntity extends TypedEntity {
    private Signature signature;
    private TypeEntity containingType;
    private PackageEntity containingPackage;
    private List<FunctionLink> links;

    public FunctionEntity(String id, String lang, String name, Signature signature, String documentation
            , TypeEntity containingType, PackageEntity containingPackage, String docLink
            , List<TypedEntity> parameters) {
        super(id, lang, name, documentation, docLink, parameters);
        this.signature = signature;
        this.containingType = containingType;
        this.containingPackage = containingPackage;
        this.links = new ArrayList<FunctionLink>();
    }

    public Signature getSignature() {
        return signature;
    }

    public TypeEntity getContainingType() {
        return containingType;
    }

    public void setContainingType(TypeEntity containingType) {
        this.containingType = containingType;
    }

    public PackageEntity getContainingPackage() {
        return containingPackage;
    }

    public void setContainingPackage(PackageEntity containingPackage) {
        this.containingPackage = containingPackage;
    }

    public List<FunctionLink> getLinks() {
        return links;
    }

    public void setSignature(Signature signature) {
        this.signature = signature;
    }

    public FunctionEntity clone() {
        return new FunctionEntity(id, lang, name, signature, documentation, containingType, containingPackage
                , docLink, parameters);
    }
}
