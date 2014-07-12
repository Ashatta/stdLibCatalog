package com.jetbrains.agapova.stdLibCatalog.domain;

import java.util.List;

/**
 * Created by ashatta on 7/12/14.
 */
public class ClassEntity extends TypeEntity {
    private List<InterfaceEntity> interfaces;

    public ClassEntity(String id, String name, String lang, String documentation, List<FunctionEntity> functions
            , List<TypeEntity> derived, List<TypeEntity> parentEntities, PackageEntity containingPackage
            , List<TypeEntity> parameters, String docLink, List<InterfaceEntity> interfaces) {
        super(id, name, lang, documentation, functions, derived, parentEntities, parameters, containingPackage
                , docLink);
        this.interfaces = interfaces;
    }

    public List<InterfaceEntity> getInterfaces() {
        return interfaces;
    }
}
