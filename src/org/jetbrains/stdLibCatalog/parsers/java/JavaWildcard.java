package org.jetbrains.stdLibCatalog.parsers.java;

import org.jetbrains.stdLibCatalog.domain.*;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.jetbrains.stdLibCatalog.parsers.java.JavaParser.BoundDescription;
import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.QualifiedName;

public class JavaWildcard extends JavaType {
    private final List<BoundDescription> bounds = new ArrayList<>();

    public static JavaType parse(Element elem, String typeString) {
        JavaWildcard result = new JavaWildcard();

        Iterator<String> partsIterator = JavaParser.typeSplit(typeString, "").iterator();
        partsIterator.next();
        BoundDescription.BoundType boundType = BoundDescription.BoundType.UPPER;
        while (partsIterator.hasNext()) {
            String part = partsIterator.next();
            if (part.equals("extends") || part.equals("super")) {
                boundType = part.equals("extends") ? BoundDescription.BoundType.UPPER : BoundDescription.BoundType.LOWER;
            } else if (!part.equals("&")) {
                result.bounds.add(new BoundDescription(boundType, JavaType.parse(elem, part)));
            }
        }

        return result;
    }

    public DataType buildType(JavaParser parser, Map<String, TypeVariable> entityParams
            , Map<String, TypeVariable> parentParams, QualifiedName enclosingClass) {
        Wildcard wildcard = new Wildcard(Language.JAVA);
        for (BoundDescription boundDesc : bounds) {
            switch (boundDesc.getKey()) {
                case UPPER: wildcard.addUpperBound(boundDesc.getValue().buildType(parser, entityParams, parentParams, enclosingClass));
                    break;
                case LOWER: wildcard.addLowerBound(boundDesc.getValue().buildType(parser, entityParams, parentParams, enclosingClass));
            }
        }
        return new DataType(wildcard, new ArrayList<Type>());
    }

    public void extractVariables(Map<String, TypeVariable> entityVars
            , Map<String, TypeVariable> parentVars, List<TypeVariable> result) {
        for (BoundDescription bound : bounds) {
            bound.getValue().extractVariables(entityVars, parentVars, result);
        }
    }

    public void extractOtherEntities(JavaParser parser, Map<String, TypeVariable> entityVars
            , Map<String, TypeVariable> parentVars, List<Entity> result) {
        for (BoundDescription bound : bounds) {
            bound.getValue().extractOtherEntities(parser, entityVars, parentVars, result);
        }
    }

    public String buildString() {
        String result = "?";
        for (BoundDescription bound : bounds) {
            result += " " + (bound.getKey() == BoundDescription.BoundType.LOWER ? "super" : "extends") + " "
                    + bound.getValue().buildString();
        }
        return result;
    }
}
