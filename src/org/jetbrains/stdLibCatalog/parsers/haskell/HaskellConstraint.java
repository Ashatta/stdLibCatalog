package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.Constraint;
import org.jetbrains.stdLibCatalog.domain.Entity;
import org.jetbrains.stdLibCatalog.domain.TypeVariable;
import org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.QualifiedName;

class HaskellConstraint {
    private static List<String> K_ALLOWED_TYPES = Arrays.asList("Ord", "Eq", "Show");

    private final List<String> variables;
    private final List<QualifiedName> otherEntities;
    private final String declaration;

    private HaskellConstraint(List<String> variables, List<QualifiedName> otherEntities, String declaration) {
        this.variables = variables;
        this.otherEntities = otherEntities;
        this.declaration = declaration;
    }

    public static HaskellConstraint parse(Element entityElem, String declaration) {
        List<String> parts = Arrays.asList(declaration.replaceAll("[\\(\\)\\[\\],\\->#]", "").split("\\s+"));
        List<String> vars = new ArrayList<>();

        boolean kAllowed = false;
        for (String part : parts) {
            if (K_ALLOWED_TYPES.contains(part)) {
                kAllowed = true;
                break;
            }
        }

        for (String part : parts) {
            if (Character.isLowerCase(part.charAt(0)) && (kAllowed || part.charAt(0) != 'k')) {
                vars.add(part);
            }
        }

        Elements entityElems = entityElem.getElementsByAttribute("href");

        List<QualifiedName> other = new ArrayList<>();
        for (Element elem : entityElems) {
            if (parts.contains(elem.text())) {
                QualifiedName entityName
                        = new QualifiedName(HaskellParser.getPackageName(elem.attr("href")), elem.text());
                if (!other.contains(entityName)) {
                    other.add(entityName);
                }
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
        for (QualifiedName entity : otherEntities) {
            if (parser.getClasses().containsKey(entity)) {
                other.add(parser.getClasses().get(entity));
            } else if (parser.getAliases().containsKey(entity)) {
                other.add(parser.getAliases().get(entity));
            } else if (parser.getFunctions().containsKey(entity)) {
                other.add(parser.getFunctions().get(entity));
            }
        }

        return new Constraint(typeVariables, other, declaration);
    }

    public String getDeclaration() {
        return declaration;
    }
}
