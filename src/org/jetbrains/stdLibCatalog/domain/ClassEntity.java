package org.jetbrains.stdLibCatalog.domain;

import java.util.List;

public class ClassEntity extends TypeEntity {
    private List<InterfaceEntity> interfaces;

    public ClassEntity(String id, String name, String lang, String documentation, List<FunctionEntity> functions
            , List<TypeEntity> derived, List<TypeEntity> parentEntities, PackageEntity containingPackage
            , List<TypedEntity> parameters, String docLink, List<InterfaceEntity> interfaces) {
        super(id, name, lang, documentation, functions, derived, parentEntities, parameters, containingPackage
                , docLink);
        this.interfaces = interfaces;
    }

    public List<InterfaceEntity> getInterfaces() {
        return interfaces;
    }

    public ClassEntity clone() {
        return new ClassEntity(id, name, lang, documentation, functions, derived, base, containingPackage
                , parameters, docLink, interfaces);
    }

    public void addInterface(InterfaceEntity interfaceEntity) {
        interfaces.add(interfaceEntity);
    }
}
