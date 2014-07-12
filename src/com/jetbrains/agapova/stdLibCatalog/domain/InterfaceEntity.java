package com.jetbrains.agapova.stdLibCatalog.domain;

import java.util.List;

/**
 * Created by ashatta on 7/12/14.
 */
public class InterfaceEntity extends TypeEntity {
    private List<ClassEntity> supportingClasses;

    public InterfaceEntity(String id, String name, String lang, String documentation, List<FunctionEntity> functions
            , List<TypeEntity> derived, List<TypeEntity> parentEntities, PackageEntity containingPackage
            , List<TypeEntity> parameters, String docLink, List<ClassEntity> supportingClasses) {
        super(id, name, lang, documentation, functions, derived, parentEntities, parameters, containingPackage
                , docLink);
        this.supportingClasses = supportingClasses;
    }

    public List<ClassEntity> getSupportingClasses() {
        return supportingClasses;
    }
}
