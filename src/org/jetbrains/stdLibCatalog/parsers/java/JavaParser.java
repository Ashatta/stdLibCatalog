package org.jetbrains.stdLibCatalog.parsers.java;

import javafx.util.Pair;
import org.jetbrains.stdLibCatalog.domain.*;
import org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.MalformedInputException;
import java.util.*;
import java.util.regex.Pattern;

import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.QualifiedName;

// todo intermediate packages
public class JavaParser {
    private static enum MemberType {
        ENUM_CONST,
        FIELD,
        METHOD
    }

    static class BoundDescription extends Pair<BoundDescription.BoundType, JavaType> {
        public BoundDescription(BoundType boundType, JavaType type) {
            super(boundType, type);
        }

        public static enum BoundType {
            LOWER,
            UPPER
        }
    }

    static class ParametersDescription extends HashMap<String, List<BoundDescription>> {
    }

    public static final String OTHER_PACKAGE = "other";
    private static final List<String> CLASSIFIER_TYPES_ATTRIBUTES = Arrays.asList("class", "@interface", "interface", "enum");
    private static final List<String> CLASS_ATTRIBUTES = Arrays.asList("final", "abstract", "static");
    private static final List<String> METHOD_ATTRIBUTES = Arrays.asList("final", "abstract", "static", "default");
    private static final List<String> FIELD_ATTRIBUTES = Arrays.asList("final", "static");
    private static final List<String> SCOPE_ATTRIBUTES = Arrays.asList("public", "protected");
    public static final List<String> PRIMITIVE_TYPES = Arrays.asList("boolean", "int", "byte", "char", "short", "long",
            "double", "float", "void");
    private static String BASE_FOLDER = "/home/ashatta/Downloads/docs/api/";
    private static String BASE_ADDRESS = "http://docs.oracle.com/javase/8/docs/api/";

    private final Map<String, PackageEntity> packages = new HashMap<>();
    private final Map<QualifiedName, Classifier> classes = new HashMap<>();
    private final Map<QualifiedName, List<TypeConstructor>> nestedClasses = new HashMap<>();
    private final Map<QualifiedName, List<JavaType>> superTypes = new HashMap<>();
    private final Map<QualifiedName, ParametersDescription> parameters = new HashMap<>();
    private final Map<QualifiedName, Map<String, ParametersDescription>> memberParameters = new HashMap<>();
    private final Map<QualifiedName, Map<String, JavaType>> fields = new HashMap<>();
    private final Map<QualifiedName, Map<String, JavaFunction>> methods = new HashMap<>();
    private final Map<MemberEntity, String> membersReverseIndex = new HashMap<>();

    public static void main(String[] args) throws IOException {
        JavaParser parser = new JavaParser();
        parser.fillEmbeddedTypes();
        parser.parse();
/*
        File dir = new File("/home/ashatta/debug");
        dir.mkdir();
        for (PackageEntity pack : parser.packages.values()) {
            File packDir = new File(dir.getAbsolutePath() + "/" + pack.getName());
            packDir.mkdir();
            File packFile = new File(packDir.getAbsolutePath() + "/" + pack.getName() + ".txt");
            packFile.createNewFile();
            FileWriter out = new FileWriter(packFile);
            out.write(pack.toString());
            out.close();

            File contentsDir = new File(packDir.getAbsolutePath() + "/contents");
            contentsDir.mkdir();
            for (TypeConstructor member : pack.getContainedClasses()) {
                File memberFile = new File(contentsDir.getAbsoluteFile() + "/" + member.getName() + ".txt");
                memberFile.createNewFile();
                out = new FileWriter(memberFile);
                out.write(member.toString());
                out.close();
            }
        }
        */
    }

    private void parse() throws IOException {
        Document document = Jsoup.parse(new File(BASE_FOLDER + "overview-summary.html"), "UTF-8");
        Element overviewTable = document.getElementsByClass("overviewSummary").first().getElementsByTag("tbody").last();
        for (Element row : overviewTable.children()) {
            parsePackage(row.child(0).child(0));
        }

        createConnections();
    }

    private void parsePackage(Element linkElem) throws IOException {
        Document document = Jsoup.parse(new File(BASE_FOLDER + linkElem.attr("href")), "UTF-8");
        String packageName = linkElem.text();

        Element docElem = document.getElementsByAttributeValue("name", "package.description").isEmpty() ? null
                : document.getElementsByAttributeValue("name", "package.description").first()
                        .nextElementSibling().nextElementSibling();
        String doc = docElem == null ? "" : docElem.text();

        PackageEntity pack = new PackageEntity(packageName, Language.JAVA, parsePackageMembers(document, packageName),
                new ArrayList<MemberEntity>(), new ArrayList<PackageEntity>(), doc,
                new URL(BASE_ADDRESS + linkElem.attr("href")));
        packages.put(packageName, pack);
    }

    private List<TypeConstructor> parsePackageMembers(Document document, String packageName) throws IOException {
        List<TypeConstructor> members = new ArrayList<>();
        for (Element typeTable : document.getElementsByClass("typeSummary")) {
            for (Element row : typeTable.getElementsByTag("tbody").last().getElementsByTag("tr")) {
                addClasses(row.child(0), members, packageName);
            }
        }
        return members;
    }

    private void addClasses(Element classElem, List<TypeConstructor> result, String packageName) throws IOException {
        Classifier classifier = parseClass(classElem);
        if (classifier != null) {
            result.add(classifier);
            result.addAll(nestedClasses.get(new QualifiedName(packageName, classifier.getName())));
        }
    }

    private Classifier parseClass(Element linkElem) throws IOException {
        String fullDecl = linkElem.text();
        String name = fullDecl.split("<")[0];
        QualifiedName qualifiedName = new QualifiedName(getPackageName(linkElem, name), name);
        if (classes.containsKey(qualifiedName)) {
            return null;
        }

        parameters.put(qualifiedName, parseParameters(linkElem, fullDecl.replaceFirst(Pattern.quote(name), "")));
        createClassMaps(qualifiedName);

        String filePath = getFilePath(linkElem, name);
        Document document = Jsoup.parse(new File(BASE_FOLDER + filePath), "UTF-8");

        Element declaration = document.getElementsByClass("description").first().getElementsByTag("pre")
                .get(linkElem.child(0).attr("href").equals("../../../javax/security/auth/Policy.html") ? 2 : 0);
        Map<String, String> attributes = new HashMap<>();
        parseClassDefinition(qualifiedName, declaration, fullDecl, attributes);

        Classifier classifier = new Classifier(name, Language.JAVA, getDoc(declaration),
                new URL(BASE_ADDRESS + filePath), parseMembers(document, qualifiedName), declaration.text());

        for (Map.Entry<String, String> attr : attributes.entrySet()) {
            classifier.setAttr(attr.getKey(), attr.getValue());
        }

        classes.put(qualifiedName, classifier);
        return classifier;
    }

    private ParametersDescription parseParameters(Element elem, String paramsDef) throws MalformedInputException {
        if (paramsDef.isEmpty()) {
            return new ParametersDescription();
        }

        paramsDef = paramsDef.substring(1, paramsDef.length() - 1);
        ParametersDescription params = new ParametersDescription();

        List<String> paramsStrings = typeSplit(paramsDef, ",");
        for (String paramDesc : paramsStrings) {
            Iterator<String> partsIterator = typeSplit(paramDesc, "").iterator();
            String paramName = partsIterator.next();
            params.put(paramName, new ArrayList<BoundDescription>());
            BoundDescription.BoundType boundType = BoundDescription.BoundType.UPPER;
            while (partsIterator.hasNext()) {
                String part = partsIterator.next();
                if (part.equals("extends") || part.equals("super")) {
                    boundType = part.equals("extends") ? BoundDescription.BoundType.UPPER : BoundDescription.BoundType.LOWER;
                } else if (!part.equals("&")) {
                    params.get(paramName).add(new BoundDescription(boundType, JavaType.parse(elem, part)));
                }
            }
        }

        return params;
    }

    private void parseClassDefinition(QualifiedName name, Element declaration, String decl, Map<String, String> attrs) {
        String fullDeclaration = declaration.text().replaceAll("\\p{javaSpaceChar}", " ");
        int declPos = fullDeclaration.indexOf(decl);

        String before = removeAnnotations(fullDeclaration.substring(0, declPos - 1));
        parseClassAttributes(before, attrs);

        List<JavaType> superTypes = new ArrayList<>();
        if (declPos + decl.length() < fullDeclaration.length()) {
            String after = fullDeclaration.substring(declPos + decl.length() + 1, fullDeclaration.length());

            String[] implementsParts = after.split("(\\s+|)implements\\s+");
            if (implementsParts.length > 1) {
                parseSuperTypes(declaration, implementsParts[implementsParts.length - 1], superTypes);
            }

            String[] extendsParts = implementsParts[0].split("(\\s+|)extends\\s+");
            if (extendsParts.length > 1) {
                parseSuperTypes(declaration, extendsParts[extendsParts.length - 1], superTypes);
            }
        }

        this.superTypes.put(name, superTypes);
    }

    private void parseClassAttributes(String attributeString, Map<String, String> attrs) {
        List<String> attributes = typeSplit(attributeString, "");
        for (String attr : CLASS_ATTRIBUTES) {
            attrs.put(attr, attributes.contains(attr) ? "true" : "false");
        }

        for (String attr : CLASSIFIER_TYPES_ATTRIBUTES) {
            if (attributes.contains(attr)) {
                attrs.put("classifierType", attr.equals("@interface") ? "annotation" : attr);
            }
        }

        attrs.put("scope", "package-local");
        for (String attr : SCOPE_ATTRIBUTES) {
            if (attributes.contains(attr)) {
                attrs.put("scope", attr);
            }
        }
    }

    private void parseSuperTypes(Element declaration, String superTypesString, List<JavaType> superTypes) {
        List<String> interfaces = typeSplit(superTypesString, ",");
        for (String str : interfaces) {
            superTypes.add(JavaType.parse(declaration, str));
        }
    }

    private List<MemberEntity> parseMembers(Document document, QualifiedName enclosingClass) throws IOException {
        List<MemberEntity> result = new ArrayList<>();

        for (Element nestedClassSummary : document.getElementsByAttributeValue("name", "nested.class.summary")) {
            parseNestedClasses(nestedClassSummary.nextElementSibling().nextElementSibling(), enclosingClass);
        }

        for (Element enumConstantDetail : document.getElementsByAttributeValue("name", "enum.constant.detail")) {
            result.addAll(parseClassMembers(enumConstantDetail, enclosingClass, MemberType.ENUM_CONST));
        }

        for (Element fieldDetail : document.getElementsByAttributeValue("name", "field.detail")) {
            result.addAll(parseClassMembers(fieldDetail, enclosingClass, MemberType.FIELD));
        }

        for (Element constructorDetail : document.getElementsByAttributeValue("name", "constructor.detail")) {
            result.addAll(parseClassMembers(constructorDetail, enclosingClass, MemberType.METHOD));
        }

        for (Element methodDetail : document.getElementsByAttributeValue("name", "method.detail")) {
            result.addAll(parseClassMembers(methodDetail, enclosingClass, MemberType.METHOD));
        }

        return result;
    }

    private void parseNestedClasses(Element classesTable, QualifiedName enclosingClass) throws IOException {
        for (Element row : classesTable.getElementsByTag("tr")) {
            Element elem = row.child(1);
            if (elem.tagName().equals("td")) {
                addClasses(elem.child(0), nestedClasses.get(enclosingClass), enclosingClass.getKey());
            }
        }
    }

    private List<MemberEntity> parseClassMembers(Element memberDetail, QualifiedName enclosingClass, MemberType type)
            throws MalformedURLException, MalformedInputException {
        List<MemberEntity> result = new ArrayList<>();

        memberDetail = memberDetail.nextElementSibling().nextElementSibling();
        while (memberDetail != null) {
            MemberEntity member = null;

            switch (type) {
                case ENUM_CONST: member = parseField(memberDetail, enclosingClass, true);
                    break;
                case FIELD: member = parseField(memberDetail, enclosingClass, false);
                    break;
                case METHOD: member = parseMethod(memberDetail, enclosingClass);
                    break;
            }

            if (member != null) {
                result.add(member);
                memberDetail = memberDetail.nextElementSibling();
            }
            memberDetail = memberDetail.nextElementSibling();
        }

        return result;
    }

    private MemberEntity parseField(Element fieldDetail, QualifiedName enclosingClass, boolean isEnumConst)
            throws MalformedURLException {
        String addressName = fieldDetail.attr("name");
        memberParameters.get(enclosingClass).put(addressName, new ParametersDescription());

        fieldDetail = fieldDetail.nextElementSibling();
        if (fieldDetail == null || fieldDetail.tagName().equals("a")) {
            return null;
        }

        Element declaration = fieldDetail.getElementsByTag("pre").first();
        List<String> parts = typeSplit(removeAnnotations(declaration.text().replaceAll("\\p{javaSpaceChar}", " ")), "");

        String name = parts.get(parts.size() - 1);
        JavaType type = JavaType.parse(declaration, parts.get(parts.size() - 2));

        MemberEntity field = new MemberEntity(name, Language.JAVA, getDoc(declaration),
                new URL(BASE_ADDRESS + memberPath(enclosingClass, addressName)), declaration.text());
        fillFieldAttributes(field, parts.subList(0, parts.size() - 2), isEnumConst);

        fields.get(enclosingClass).put(addressName, type);
        membersReverseIndex.put(field, addressName);
        return field;
    }

    private void fillFieldAttributes(MemberEntity field, List<String> attributes, boolean isEnumConst) {
        field.setAttr("memberType", isEnumConst ? "enumConst" : "field");
        for (String attr : FIELD_ATTRIBUTES) {
            field.setAttr(attr, attributes.contains(attr) ? "true" : "false");
        }

        field.setAttr("scope", "package-local");
        for (String attr : SCOPE_ATTRIBUTES) {
            if (attributes.contains(attr)) {
                field.setAttr("scope", attr);
            }
        }
    }

    private MemberEntity parseMethod(Element methodDetail, QualifiedName enclosingClass) throws MalformedURLException, MalformedInputException {
        String addressName = methodDetail.attr("name");
        memberParameters.get(enclosingClass).put(addressName, new ParametersDescription());

        methodDetail = methodDetail.nextElementSibling();
        if (methodDetail == null || methodDetail.tagName().equals("a")) {
            return null;
        }

        Element declaration = methodDetail.getElementsByTag("pre").first();
        String fullDeclaration = removeAnnotations(declaration.text().replaceAll("\\p{javaSpaceChar}", " "));
        List<String> withoutParameters = typeSplit(fullDeclaration.substring(0, fullDeclaration.indexOf('(')), "");

        String name = withoutParameters.get(withoutParameters.size() - 1);
        String[] parts = enclosingClass.getValue().split("\\.");
        boolean constructor = name.equals(parts[parts.length - 1]);

        String signatureString = withoutParameters.get(withoutParameters.size() - (constructor ? 1 : 2))
                + fullDeclaration.substring(fullDeclaration.indexOf('('), fullDeclaration.lastIndexOf(')') + 1);
        JavaFunction func = JavaFunction.parse(declaration, signatureString);

        MemberEntity function = new MemberEntity(name, Language.JAVA, getDoc(declaration),
                new URL(BASE_ADDRESS + memberPath(enclosingClass, addressName)), declaration.text());
        fillMethodAttributes(function, withoutParameters.subList(0, withoutParameters.size() - (constructor ? 1 : 2)),
                constructor);

        for (String str : withoutParameters) {
            if (str.startsWith("<")) {
                memberParameters.get(enclosingClass).put(addressName, parseParameters(declaration, str));
            }
        }

        methods.get(enclosingClass).put(addressName, func);
        membersReverseIndex.put(function, addressName);
        return function;
    }

    private void fillMethodAttributes(MemberEntity method, List<String> attributes, boolean constructor)
            throws MalformedInputException {
        method.setAttr("constructor", constructor ? "true" : "false");
        method.setAttr("memberType", "method");
        for (String attr : METHOD_ATTRIBUTES) {
            method.setAttr(attr, attributes.contains(attr) ? "true" : "false");
        }

        method.setAttr("scope", "package-local");
        for (String attr : SCOPE_ATTRIBUTES) {
            if (attributes.contains(attr)) {
                method.setAttr("scope", attr);
            }
        }
    }

    private void createConnections() {
        buildPackageHierarchy();

        for (PackageEntity pack : packages.values()) {
            connectContaining(pack);
        }

        for (Map.Entry<QualifiedName, Classifier> classEntity : classes.entrySet()) {
            connectClass(packages.get(classEntity.getKey().getKey()), classEntity.getValue());
        }
    }

    private void connectContaining(PackageEntity pack) {
        for (TypeConstructor classEntity : pack.getContainedClasses()) {
            classEntity.setContainingPackage(pack);
            Classifier classifier = (Classifier) classEntity;
            for (MemberEntity member : classifier.getMembers()) {
                member.setContainingPackage(pack);
                member.setContainingType(classifier);
            }
        }
    }

    private void buildPackageHierarchy() {
        for (PackageEntity pack : packages.values()) {
            String name = pack.getName();
            if (name.equals(OTHER_PACKAGE)) {
                continue;
            }

            String parentName = name.substring(0, name.lastIndexOf('.'));
            while (!packages.containsKey(parentName) && parentName.contains(".")) {
                parentName = parentName.substring(0, parentName.lastIndexOf('.'));
            }

            if (packages.containsKey(parentName)) {
                packages.get(name).setContainingPackage(packages.get(parentName));
                packages.get(parentName).addSubPackage(packages.get(name));
            }
        }
    }

    private void connectClass(PackageEntity pack, Classifier classEntity) {
        QualifiedName name = new QualifiedName(pack.getName(), classEntity.getName());

        Map<String, TypeVariable> variables = getTypeParameters(parameters.get(name));
        for (TypeVariable variable : variables.values()) {
            classEntity.addParameter(variable);
        }

        buildSuperTypes(classEntity, variables);

        for (MemberEntity member : classEntity.getMembers()) {
            if (classEntity.getAttr("classifierType").equals("interface")) {
                member.setAttr("scope", "public");
            }
            connectMember(member, variables, name);
        }
    }

    private void buildSuperTypes(Classifier classEntity, Map<String, TypeVariable> variables) {
        QualifiedName name = new QualifiedName(classEntity.getContainingPackage().getName(), classEntity.getName());

        for (JavaType superType : superTypes.get(name)) {
            DataType type = superType.buildType(this, variables, variables, name);
            classEntity.addBase(type);
            Classifier base = (Classifier) type.getTypeConstructor();
            if (base != null) {
                base.addDerived(classEntity);
            }
        }
    }

    private void connectMember(MemberEntity member, Map<String, TypeVariable> parentVars, QualifiedName enclosingClass) {
        String addressName = membersReverseIndex.get(member);

        Map<String, TypeVariable> vars = getTypeParameters(memberParameters.get(enclosingClass).get(addressName));
        for (TypeVariable variable : vars.values()) {
            member.addParameter(variable);
        }

        member.setSignature(member.getAttr("memberType").equals("method")
                ? methods.get(enclosingClass).get(addressName).buildFunction(this, vars, parentVars, enclosingClass)
                : fields.get(enclosingClass).get(addressName).buildAsFunction(this, vars, parentVars, enclosingClass));
    }

    private Map<String, TypeVariable> getTypeParameters(Map<String, List<BoundDescription>> params) {
        Map<String, TypeVariable> variables = new HashMap<>();
        for (Map.Entry<String, List<BoundDescription>> param : params.entrySet()) {
            variables.put(param.getKey(), new TypeVariable(param.getKey(), Language.JAVA));
        }

        List<Constraint> constraints = new ArrayList<>();
        for (Map.Entry<String, List<BoundDescription>> param : params.entrySet()) {
            for (BoundDescription constraintDescription : param.getValue()) {
                constraints.add(buildConstraint(variables.get(param.getKey()), constraintDescription, variables, variables));
            }
        }

        for (Constraint constraint : constraints) {
            for (TypeVariable variable : constraint.getVariables()) {
                variable.addConstraint(constraint);
            }
        }
        return variables;
    }

    Constraint buildConstraint(TypeVariable variable, BoundDescription constraintDescription
            , Map<String, TypeVariable> entityParams, Map<String, TypeVariable> parentParams) {
        List<TypeVariable> variables = new ArrayList<>();
        constraintDescription.getValue().extractVariables(entityParams, parentParams, variables);
        if (!contains(variables, variable)) {
            variables.add(variable);
        }

        List<Entity> otherEntities = new ArrayList<>();
        constraintDescription.getValue().extractOtherEntities(this, entityParams, parentParams, otherEntities);
        String type = constraintDescription.getKey().equals(BoundDescription.BoundType.LOWER) ? "super" : "extends";
        return new Constraint(variables, otherEntities, type + " " + constraintDescription.getValue().buildString());
    }

    private boolean contains(List<TypeVariable> variables, TypeVariable variable) {
        for (TypeVariable var : variables) {
            if (var.getName().equals(variable.getName())) {
                return true;
            }
        }

        return false;
    }

    private static String getDoc(Element declarationElem) {
        Element docElement = declarationElem.nextElementSibling();
        return docElement != null ? docElement.text() : "";
    }

    private static String removeAnnotations(String declaration) {
        return declaration.replaceAll("\\@[^\n]+\\n", "");
    }

    private static String memberPath(QualifiedName enclosingClass, String memberName) {
        return enclosingClass.getKey().replaceAll("\\.", "/") + "/" + enclosingClass.getValue() + ".html#" + memberName;
    }

    static String getPackageName(Element elem, String entityName) {
        String filePath = getFilePath(elem, entityName);
        return filePath != null ? filePath.substring(0, filePath.lastIndexOf('/')).replaceAll("/", ".") : "null";
    }

    static String getFilePath(Element linkElem, String name) {
        Elements links = linkElem.getElementsMatchingOwnText("^" + Pattern.quote(name) + '$');
        if (links.isEmpty() || !links.last().hasAttr("href")) {
            return null;
        }

        String fileRelative = links.last().attr("href");
        String[] parts = fileRelative.split("\\./");
        return parts[parts.length - 1];
    }

    static List<String> typeSplit(String signature, String str) {
        return ParserUtils.typeSplit(signature, str, Arrays.asList('<'), Arrays.asList('>'));
    }

    private void fillEmbeddedTypes() {
        // todo array members and variables
        List<String> otherClassesNames = new ArrayList<>(PRIMITIVE_TYPES);
        otherClassesNames.add("[]");
        otherClassesNames.add("...");

        List<TypeConstructor> otherClasses = new ArrayList<>();
        for (String type : otherClassesNames) {
            Classifier classifier = new Classifier(type, Language.JAVA, "", null, new ArrayList<MemberEntity>(), "");
            otherClasses.add(classifier);
            QualifiedName name = new QualifiedName(OTHER_PACKAGE, type);
            classes.put(name, classifier);
            parameters.put(name, new ParametersDescription());
            superTypes.put(name, new ArrayList<JavaType>());
            createClassMaps(name);
        }

        PackageEntity otherPackage = new PackageEntity(OTHER_PACKAGE, Language.JAVA, otherClasses,
                new ArrayList<MemberEntity>(), new ArrayList<PackageEntity>(), "", null);
        packages.put(OTHER_PACKAGE, otherPackage);

        for (TypeConstructor otherClass : otherClasses) {
            otherClass.setContainingPackage(otherPackage);
        }
    }

    private void createClassMaps(QualifiedName className) {
        fields.put(className, new HashMap<String, JavaType>());
        methods.put(className, new HashMap<String, JavaFunction>());
        memberParameters.put(className, new HashMap<String, ParametersDescription>());
        nestedClasses.put(className, new ArrayList<TypeConstructor>());
    }

    public Map<QualifiedName, Classifier> getClasses() {
        return classes;
    }

    public Map<QualifiedName, ParametersDescription> getParameters() {
        return parameters;
    }
}
