package org.jetbrains.stdLibCatalog.parsers.scala;

import org.jetbrains.stdLibCatalog.domain.Constraint;
import org.jetbrains.stdLibCatalog.domain.Entity;
import org.jetbrains.stdLibCatalog.domain.TypeVariable;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.QualifiedName;

public class ScalaConstraint {
    private final List<String> variables = new ArrayList<>();
    private final List<QualifiedName> other = new ArrayList<>();
    private String definition;

    public static ScalaConstraint parse(Element elem, String constraintString) {
        ScalaConstraint result = new ScalaConstraint();
        result.definition = constraintString;

        constraintString = constraintString
                .replaceAll("(\\[|\\]|\\s+with\\s+|\\s+<:\\s+|\\s+>:\\s+|\\s+extends\\s+)", " ")
                .replaceAll(",", "");
        String[] entities = constraintString.split("\\s+");

        for (String name : entities) {
            if (name.startsWith("_")) {
                continue;
            }

            String packageName = ScalaParser.getPackageName(elem, name);
            if (packageName == null) {
                if (!result.variables.contains(name)) {
                    result.variables.add(name);
                }
            } else {
                QualifiedName qualifiedName = new QualifiedName(packageName, name);
                if (!result.other.contains(qualifiedName)) {
                    result.other.add(qualifiedName);
                }
            }
        }

        return result;
    }

    public Constraint buildConstraint(Map<String, TypeVariable> allVars, ScalaParser parser) {
        List<TypeVariable> vars = new ArrayList<>();
        for (String var : variables) {
            if (allVars.containsKey(var)) {
                vars.add(allVars.get(var));
            }
        }

        List<Entity> otherEntities = new ArrayList<>();
        for (QualifiedName entity : other) {
            if (parser.getClasses().containsKey(entity)) {
                otherEntities.add(parser.getClasses().get(entity));
            } else if (parser.getAliases().containsKey(entity)) {
                otherEntities.add(parser.getAliases().get(entity));
            }
        }

        return new Constraint(vars, otherEntities, definition);
    }
}
