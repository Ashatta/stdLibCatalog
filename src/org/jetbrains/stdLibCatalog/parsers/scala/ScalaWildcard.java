package org.jetbrains.stdLibCatalog.parsers.scala;

import javafx.util.Pair;
import org.jetbrains.stdLibCatalog.domain.*;
import org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ScalaWildcard extends ScalaType {
    public static class BoundDescription extends Pair<BoundDescription.BoundType, ScalaType> {
        public BoundDescription(BoundType boundType, ScalaType type) {
            super(boundType, type);
        }

        public static enum BoundType {
            LOWER,
            UPPER
        }
    }

    private final List<BoundDescription> bounds = new ArrayList<>();

    public static ScalaWildcard parse(Element elem, String typeString) {
        ScalaWildcard result = new ScalaWildcard();

        Iterator<String> partsIterator = ScalaParser.typeSplit(typeString, "").iterator();
        String first = partsIterator.next();
        if (!first.equals("_")) {
            return null;
        }

        BoundDescription.BoundType boundType = BoundDescription.BoundType.UPPER;
        while (partsIterator.hasNext()) {
            String part = partsIterator.next();
            if (part.equals("<:") || part.equals(">:")) {
                boundType = part.equals("<:") ? BoundDescription.BoundType.UPPER : BoundDescription.BoundType.LOWER;
            } else if (!part.equals(",") && !part.equals("with")) {
                result.bounds.add(new BoundDescription(boundType, ScalaType.parse(elem, part)));
            }
        }

        return result;
    }

    public Classifier getClassifier(ScalaParser parser) {
        return null;
    }

    public DataType buildType(ScalaParser parser, ParserUtils.QualifiedName className, Map<String, TypeVariable> typeVariables) {
        Wildcard wildcard = new Wildcard(Language.SCALA);
        for (BoundDescription boundDesc : bounds) {
            switch (boundDesc.getKey()) {
                case UPPER: wildcard.addUpperBound(boundDesc.getValue().buildType(parser, className, typeVariables));
                    break;
                case LOWER: wildcard.addLowerBound(boundDesc.getValue().buildType(parser, className, typeVariables));
            }
        }
        return new DataType(wildcard, new ArrayList<Type>());
    }
}
