package org.jetbrains.stdLibCatalog.domain;

import org.jsoup.helper.StringUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Classifier extends TypeConstructor {
    private final List<MemberEntity> members;
    private final List<Classifier> derived = new ArrayList<>();
    private final List<Type> base = new ArrayList<>();
    private final List<TypeVariable> parameters = new ArrayList<>();
    private final String definition;

    public Classifier(String name, Language lang, String documentation, URL docLink, List<MemberEntity> members,
            String definition) {
        super(name, lang, documentation, docLink);
        this.members = members;
        this.definition = definition;
    }

    public List<MemberEntity> getMembers() {
        return members;
    }

    public void addDerived(Classifier d) {
        if (!derived.contains(d)) {
            derived.add(d);
        }
    }

    public void addBase(Type b) {
        if (!base.contains(b)) {
            base.add(b);
        }
    }

    public List<TypeVariable> getParameters() {
        return parameters;
    }

    public void addParameter(TypeVariable param) {
        parameters.add(param);
    }

    public String getDefinition() {
        return definition;
    }

    public String toString() {
        String result = "[Classifier]\n" + definition + "\n" + super.toString() + "\nparameters {";
        for (TypeVariable param : parameters) {
            result += "\n\t" + param.toString();
        }
        result += "\n}";

        result += "\nderived {";
        for (Classifier child : derived) {
            result += "\n\t" + (child.getContainingPackage() != null ? child.getContainingPackage().getName() : "null")
                    + "::" + child.getName();
        }
        result += "\n}";

        result += "\nsupertypes {";
        for (Type superType : base) {
            result += "\n\t" + StringUtil.join(Arrays.asList(superType.toString().split("\n")), "\n\t");
        }
        result += "\n}";

        result += "\nmembers {";
        for (MemberEntity member : members) {
            result += "\n\n\t" + StringUtil.join(Arrays.asList(member.toString().split("\n")), "\n\t");
        }
        result += "\n}";

        return result;
    }

    public List<Type> getBase() {
        return base;
    }
}
