package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.Constraint;
import org.jetbrains.stdLibCatalog.domain.Entity;
import org.jetbrains.stdLibCatalog.domain.TypeVariable;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

class HaskellConstraint {
    private final List<String> variables;
    private final List<HaskellParser.QualifiedName> otherEntities;
    private final String declaration;

    private HaskellConstraint(List<String> variables, List<HaskellParser.QualifiedName> otherEntities, String declaration) {
        this.variables = variables;
        this.otherEntities = otherEntities;
        this.declaration = declaration;
    }

    public static HaskellConstraint parse(Element entityElem, String declaration) {
        String[] parts = declaration.replaceAll("[\\(\\)\\[\\],\\->]", "").split("\\s+");
        List<String> vars = new ArrayList<>();
        for (String part : parts) {
            if (Character.isLowerCase(part.charAt(0)) && part.charAt(0) != 'k') {
                vars.add(part);
            }
        }

        Elements entityElems = entityElem.getElementsByAttribute("href");
        String beforeArrow = HaskellType.typeSplit(entityElem.text(), "=>").get(0);

        List<HaskellParser.QualifiedName> other = new ArrayList<>();
        for (Element elem : entityElems) {
            if (beforeArrow.contains(elem.text())) {
                other.add(new HaskellParser.QualifiedName(HaskellParser.getPackageName(elem.attr("href")), elem.text()));
            }
        }

        return new HaskellConstraint(vars, other, declaration);
    }

    public static void parseConstraints(Element elem, String description, List<HaskellConstraint> constraints) {
        if (description.startsWith("(") && description.endsWith(")")) {
            description = description.substring(1, description.length() - 1);
        }

        List<String> constraintStrings = HaskellType.typeSplit(description, ",");
        for (String constraint: constraintStrings) {
            constraints.add(parse(elem, constraint));
        }
    }

    public Constraint buildConstraint(HaskellParser parser, List<TypeVariable> variables) {
        List<TypeVariable> typeVariables = new ArrayList<>();
        if (variables != null) {
            for (TypeVariable variable : variables) {
                if (this.variables.contains(variable.getName())) {
                    typeVariables.add(variable);
                }
            }
        }

        List<Entity> other = new ArrayList<>();
        for (HaskellParser.QualifiedName entity : otherEntities) {
            if (parser.classes.containsKey(entity)) {
                other.add(parser.classes.get(entity));
            } else if (parser.aliases.containsKey(entity)) {
                other.add(parser.aliases.get(entity));
            } else if (parser.functions.containsKey(entity)) {
                other.add(parser.functions.get(entity));
            }
        }

        return new Constraint(typeVariables, other, declaration);
    }
}
