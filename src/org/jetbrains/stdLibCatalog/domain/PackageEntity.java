package org.jetbrains.stdLibCatalog.domain;

import java.net.URL;
import java.util.List;

public class PackageEntity extends Entity {

    private final List<TypeConstructor> containedClasses;
    private final List<MemberEntity> members;
    private final List<PackageEntity> subPackages;

    public PackageEntity(String name, Language lang, List<TypeConstructor> containedClasses,
            List<MemberEntity> members, List<PackageEntity> subPackages, String documentation, URL docLink) {
        super(name, lang, documentation, docLink);
        this.containedClasses = containedClasses;
        this.members = members;
        this.subPackages = subPackages;
    }
    public List<TypeConstructor> getContainedClasses() {
        return containedClasses;
    }

    public List<MemberEntity> getMembers() {
        return members;
    }

    public List<PackageEntity> getSubPackages() {
        return subPackages;
    }

    public void addSubPackage(PackageEntity subPackage) {
        subPackages.add(subPackage);
    }

    public void addContainedClass(TypeConstructor containedClass) {
        containedClasses.add(containedClass);
    }

    public void addMember(MemberEntity member) {
        members.add(member);
    }

    public String toString() {
        String result = "[Package]\n" + super.toString() + "\n\nSubpackages {";

        for (PackageEntity subPackage : subPackages) {
            result += "\n\t" + subPackage.getName();
        }
        result += "\n}";

        for (TypeConstructor constructor : containedClasses) {
            result += "\n\n" + constructor.toString();
        }

        for (MemberEntity member : members) {
            if (member.getContainingType() == null)
                result += "\n\n" + member.toString();
        }

        return result;
    }
}
