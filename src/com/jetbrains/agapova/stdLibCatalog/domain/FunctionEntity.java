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

    public PackageEntity getContainingPackage() {
        return containingPackage;
    }

    public List<FunctionLink> getLinks() {
        return links;
    }
}
