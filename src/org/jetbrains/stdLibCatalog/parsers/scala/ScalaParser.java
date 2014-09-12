package org.jetbrains.stdLibCatalog.parsers.scala;

import org.jetbrains.stdLibCatalog.domain.*;
import org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.QualifiedName;
import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.splitByBraces;

public class ScalaParser {
    private final int CONNECTION_TIMEOUT = 2000;
    static final String OTHER_PACKAGE = "other";
    static final String NON_EXISTING_PACKAGE = "non-existing";
    private final String BASE_ADDRESS = "http://www.scala-lang.org/api/current/";
    private String currentAddress = "";
    private final List<String> CLASSIFIER_ATTRIBUTES = Arrays.asList("abstract", "final", "implicit", "sealed");
    private final List<String> ALIAS_ATTRIBUTES = Arrays.asList("abstract");
    private final List<String> MEMBER_ATTRIBUTES = Arrays.asList("abstract", "final", "implicit", "lazy");

    private final Map<QualifiedName, Classifier> classes = new HashMap<>();
    private final Map<QualifiedName, TypeAlias> aliases = new HashMap<>();
    private final Map<QualifiedName, List<String>> classParameters = new HashMap<>();
    private final Map<QualifiedName, Map<String, TypeVariable>> classEndParameters = new HashMap<>();
    private final Map<QualifiedName, List<ScalaConstraint>> classParametersConstraints = new HashMap<>();
    private final Map<QualifiedName, List<ScalaType>> superTypes = new HashMap<>();
    private final Map<QualifiedName, ScalaType> aliasedTypes = new HashMap<>();
    private final Map<QualifiedName, Map<String, List<String>>> memberParameters = new HashMap<>();
    private final Map<QualifiedName, Map<String, Map<String, TypeVariable>>> memberEndParameters = new HashMap<>();
    private final Map<QualifiedName, Map<String, List<ScalaConstraint>>> memberParametersConstraints = new HashMap<>();
    private final Map<QualifiedName, Map<String, ScalaType>> memberTypes = new HashMap<>();
    private final Map<QualifiedName, Map<String, MemberEntity>> classMembers = new HashMap<>();
    private final Map<String, PackageEntity> packages = new HashMap<>();
    private final Map<QualifiedName, QualifiedName> enclosingClasses = new HashMap<>();
    private final Map<Classifier, QualifiedName> namesReverseIndex = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ScalaParser parser = new ScalaParser();
        parser.fillOther();
        parser.currentAddress = "http://www.scala-lang.org/api/current/scala/package.html";
        parser.parsePackage();

        parser.createConnections();
        System.out.println();

        File dir = new File("resources/tests/parsers/scala/global");
        if (dir.exists()) {
            dir.delete();
        }
        dir.mkdir();
        for (PackageEntity pack : parser.packages.values()) {
            File packFile = new File(dir.getAbsolutePath() + "/" + pack.getName() + ".txt");
            packFile.createNewFile();
            FileWriter out = new FileWriter(packFile);
            out.write(pack.toString());
            out.close();
        }
    }

    private void createConnections() {
        createClassConnections();
        createAliasConnections();
        createMemberConnections();
        createPackageHierarchy();
    }

    private void createClassConnections() {
        for (Map.Entry<QualifiedName, Classifier> type : classes.entrySet()) {
            type.getValue().setContainingPackage(packages.get(type.getKey().getKey()));
            packages.get(type.getKey().getKey()).addContainedClass(type.getValue());
            createTypeVariables(type);
        }

        createConstraints();

        for (Map.Entry<QualifiedName, List<ScalaType>> classSuperTypes : superTypes.entrySet()) {
            Classifier type = classes.get(classSuperTypes.getKey());
            QualifiedName name = namesReverseIndex.get(type);
            for (ScalaType superType : classSuperTypes.getValue()) {
                Classifier superTypeClassifier = superType.getClassifier(this);
                type.addBase(superType.buildType(this, name, getAllClassVars(name)));
                if (superTypeClassifier != null) {
                    superTypeClassifier.addDerived(type);
                }
            }
        }
    }

    private void createTypeVariables(Map.Entry<QualifiedName, Classifier> type) {
        classEndParameters.put(type.getKey(), new HashMap<String, TypeVariable>());
        for (String param : classParameters.get(type.getKey())) {
            TypeVariable var = new TypeVariable(param, Language.SCALA);
            classEndParameters.get(type.getKey()).put(param, var);
            type.getValue().addParameter(var);
        }
    }

    private void createConstraints() {
        for (Map.Entry<QualifiedName, Classifier> type : classes.entrySet()) {
            for (ScalaConstraint constraint : classParametersConstraints.get(type.getKey())) {
                Constraint constr = constraint.buildConstraint(getAllClassVars(type.getKey()), this);
                for (TypeVariable var : constr.getVariables()) {
                    if (!constr.getDeclaration().equals(var.getName())) {
                        var.addConstraint(constr);
                    }
                }
            }
        }
    }

    private void createAliasConnections() {
        for (Map.Entry<QualifiedName, TypeAlias> type : aliases.entrySet()) {
            type.getValue().setContainingPackage(packages.get(type.getKey().getKey()));
            packages.get(type.getKey().getKey()).addContainedClass(type.getValue());
            createTypeAliasVariables(type);
        }

        createAliasConstraints();

        for (Map.Entry<QualifiedName, TypeAlias> type : aliases.entrySet()) {
            type.getValue().setAliasedType(aliasedTypes.get(type.getKey()).buildType(this,
                    new QualifiedName(NON_EXISTING_PACKAGE, ""), getAllClassVars(type.getKey())));
        }
    }

    private void createAliasConstraints() {
        for (Map.Entry<QualifiedName, TypeAlias> type : aliases.entrySet()) {
            for (ScalaConstraint constraint : classParametersConstraints.get(type.getKey())) {
                Constraint constr = constraint.buildConstraint(getAllClassVars(type.getKey()), this);
                for (TypeVariable var : constr.getVariables()) {
                    if (!constr.getDeclaration().equals(var.getName())) {
                        var.addConstraint(constr);
                    }
                }
            }
        }
    }

    private void createTypeAliasVariables(Map.Entry<QualifiedName, TypeAlias> type) {
        classEndParameters.put(type.getKey(), new HashMap<String, TypeVariable>());
        for (String param : classParameters.get(type.getKey())) {
            TypeVariable var = new TypeVariable(param, Language.SCALA);
            classEndParameters.get(type.getKey()).put(param, var);
            type.getValue().addParameter(var);
        }
    }

    private void createMemberConnections() {
        for (Map.Entry<QualifiedName, Map<String, MemberEntity>> members : classMembers.entrySet()) {
            for (Map.Entry<String, MemberEntity> member : members.getValue().entrySet()) {
                member.getValue().setContainingPackage(packages.get(members.getKey().getKey()));
                if (members.getKey().getValue().isEmpty()) {
                    packages.get(members.getKey().getKey()).addMember(member.getValue());
                }
                member.getValue().setContainingType(members.getKey().getValue().isEmpty() ? null : classes.get(members.getKey()));
                createMemberVariables(members.getKey(), member);
            }
        }

        createMemberConstraints();

        for (Map.Entry<QualifiedName, Map<String, MemberEntity>> members : classMembers.entrySet()) {
            for (Map.Entry<String, MemberEntity> member : members.getValue().entrySet()) {
                Map<String, TypeVariable> allVars = getAllClassVars(members.getKey());
                allVars.putAll(memberEndParameters.get(members.getKey()).get(member.getKey()));
                member.getValue().setSignature(memberTypes.get(members.getKey()).get(
                        member.getKey()).buildFunction(this, members.getKey(), allVars));
            }
        }
    }

    private void createMemberVariables(QualifiedName className, Map.Entry<String, MemberEntity> member) {
        if (!memberEndParameters.containsKey(className)) {
            memberEndParameters.put(className, new HashMap<String, Map<String, TypeVariable>>());
        }

        memberEndParameters.get(className).put(member.getKey(), new HashMap<String, TypeVariable>());
        for (String param : memberParameters.get(className).get(member.getKey())) {
            TypeVariable var = new TypeVariable(param, Language.SCALA);
            memberEndParameters.get(className).get(member.getKey()).put(param, var);
            member.getValue().addParameter(var);
        }
    }

    private void createMemberConstraints() {
        for (Map.Entry<QualifiedName, Map<String, MemberEntity>> members : classMembers.entrySet()) {
            for (Map.Entry<String, MemberEntity> member : members.getValue().entrySet()) {
                for (ScalaConstraint constraint : memberParametersConstraints.get(members.getKey()).get(member.getKey())) {
                    Map<String, TypeVariable> allVars = getAllClassVars(members.getKey());
                    allVars.putAll(memberEndParameters.get(members.getKey()).get(member.getKey()));
                    Constraint constr = constraint.buildConstraint(allVars, this);
                    for (TypeVariable var : constr.getVariables()) {
                        if (!constr.getDeclaration().equals(var.getName())) {
                            var.addConstraint(constr);
                        }
                    }
                }
            }
        }
    }

    private void createPackageHierarchy() {
        for (PackageEntity pack : packages.values()) {
            String name = pack.getName();
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                String parentName = name.substring(0, dot);
                packages.get(parentName).addSubPackage(pack);
                pack.setContainingPackage(packages.get(parentName));
            }
        }
    }

    private Map<String, TypeVariable> getAllClassVars(QualifiedName className) {
        Map<String, TypeVariable> result = new HashMap<>();
        while (!className.getValue().isEmpty()) {
            result.putAll(classEndParameters.get(className));
            className = enclosingClasses.get(className);
        }

        return result;
    }

    private void parsePackage() throws IOException {
        Document document = Jsoup.parse(new URL(currentAddress), CONNECTION_TIMEOUT);

        String name = currentAddress.replaceAll(Pattern.quote(BASE_ADDRESS), "")
                .replaceAll("/package\\.html$", "").replaceAll("/", ".");
        QualifiedName fakeClass = new QualifiedName(name, "");

        initClass(fakeClass);
        parseInnerTypes(document, fakeClass);
        packages.put(name, new PackageEntity(name, Language.SCALA, new ArrayList<TypeConstructor>(),
                parseMembers(document, fakeClass, ""), new ArrayList<PackageEntity>(),
                document.getElementById("comment").text(), getLink(document.getElementById("definition"))));
    }

    private void parseClass(QualifiedName enclosingClass) throws IOException {
        Document document = Jsoup.parse(new URL(currentAddress), CONNECTION_TIMEOUT);
        Element signatureElem = document.getElementById("signature");

        String classQualifiedName = currentAddress.replaceAll(Pattern.quote(BASE_ADDRESS), "")
                .replaceAll("\\.html$", "").replaceAll("/", ".");
        int lastSeparator = classQualifiedName.lastIndexOf('.');
        String name = signatureElem.getElementsByClass("name").get(0).text();
        String addressName = classQualifiedName.substring(lastSeparator + 1, classQualifiedName.length());
        addressName = addressName.replaceAll("package\\$+", "");
        String packageName = classQualifiedName.substring(0, lastSeparator);
        QualifiedName qualifiedName = new QualifiedName(packageName, addressName);
        if (classes.containsKey(qualifiedName)) {
            return;
        }

        initClass(qualifiedName);

        List<String> parts = typeSplit(signatureElem.text().split("\\s+extends\\s+")[0], name);
        parseTypeParameters(signatureElem, classParameters.get(qualifiedName),
                classParametersConstraints.get(qualifiedName), parts.get(parts.size() - 1).replaceAll("\\(.*\\)$", ""));

        List<String> parts2 = typeSplit(signatureElem.text(), "with");
        parseSuperTypes(qualifiedName, signatureElem, parts2);
        parts2 = typeSplit(parts2.get(0), "extends");
        parseSuperTypes(qualifiedName, signatureElem, parts2);

        enclosingClasses.put(qualifiedName, enclosingClass);

        Classifier classifier = new Classifier(name, Language.SCALA, document.getElementById("comment").text(),
                getLink(document.getElementById("definition")), parseMembers(document, qualifiedName, name),
                signatureElem.text());
        classes.put(qualifiedName, classifier);
        namesReverseIndex.put(classifier, qualifiedName);
        classifier.setAttr("classifierType", signatureElem.getElementsByClass("kind").get(0).text());
        String attrString = signatureElem.getElementsByClass("modifier").get(0).text().trim();
        for (Map.Entry<String, String> attr : parseAttributes(attrString, CLASSIFIER_ATTRIBUTES).entrySet()) {
            classifier.setAttr(attr.getKey(), attr.getValue());
        }

        parseInnerTypes(document, qualifiedName);
    }

    private void parseTypeParameters(Element signature, List<String> parameters,
            List<ScalaConstraint> parametersConstraints, String paramsString) {
        if (!paramsString.startsWith("[") || !paramsString.endsWith("]")) {
            return;
        }
        paramsString = paramsString.substring(1, paramsString.length() - 1);

        List<String> params = typeSplit(paramsString, ",");
        for (String paramString : params) {
            if (paramString.startsWith("-") || paramString.startsWith("+")) {
                paramString = paramString.substring(1, paramString.length());
            }
            parameters.add(paramString.split("\\[|\\s")[0]);
            parametersConstraints.add(ScalaConstraint.parse(signature, paramString));
        }
    }

    private void parseSuperTypes(QualifiedName className, Element signature, List<String> parts) {
        for (int i = 1; i < parts.size(); ++i) {
            superTypes.get(className).add(ScalaType.parse(signature, parts.get(i)));
        }
    }

    private Map<String, String> parseAttributes(String attrString, List<String> attributeNames) {
        Map<String, String> result = new HashMap<>();
        List<String> attrs = typeSplit(attrString, "");
        for (String attr : attributeNames) {
            result.put(attr, attrs.contains(attr) ? "true" : "false");
        }
        return result;
    }

    private void parseInnerTypes(Document document, QualifiedName className) throws IOException {
        Element typesElem = document.getElementById("types");
        if (typesElem == null) {
            return;
        }

        for (Element memberType : typesElem.getElementsByClass("signature")) {
            String kind = memberType.getElementsByClass("kind").get(0).text();
            if (kind.equals("class") || kind.equals("trait") || kind.equals("case class")) {
                Element nameElem = memberType.getElementsByClass("name").get(0);
                String link = nameElem.parent().attr("href");
                if (!link.isEmpty()) {
                    String oldAddress = currentAddress;
                    currentAddress = getNewAddress(link);
                    parseClass(className);
                    currentAddress = oldAddress;
                } else {
                    String name = nameElem.text();
                    QualifiedName qualifiedName = new QualifiedName(className.getKey(), className.getValue() + "$" + name);
                    if (classes.containsKey(qualifiedName)) {
                        return;
                    }

                    initClass(qualifiedName);

                    List<String> parts = typeSplit(memberType.text().split("\\s+extends\\s+")[0], name);
                    parseTypeParameters(memberType, classParameters.get(qualifiedName),
                            classParametersConstraints.get(qualifiedName), parts.get(parts.size() - 1).replaceAll("\\(.*\\)$", ""));

                    List<String> parts2 = typeSplit(memberType.text(), "with");
                    parseSuperTypes(qualifiedName, memberType, parts2);
                    parts2 = typeSplit(parts2.get(0), "extends");
                    parseSuperTypes(qualifiedName, memberType, parts2);

                    enclosingClasses.put(qualifiedName, className);

                    Classifier classifier = new Classifier(name, Language.SCALA, getDoc(memberType),
                            getLink(memberType.nextElementSibling()), new ArrayList<MemberEntity>(), memberType.text());
                    classes.put(qualifiedName, classifier);
                    namesReverseIndex.put(classifier, qualifiedName);
                    classifier.setAttr("classifierType", memberType.getElementsByClass("kind").get(0).text());
                    String attrString = memberType.getElementsByClass("modifier").get(0).text().trim();
                    for (Map.Entry<String, String> attr : parseAttributes(attrString, CLASSIFIER_ATTRIBUTES).entrySet()) {
                        classifier.setAttr(attr.getKey(), attr.getValue());
                    }
                }
            } else if (kind.equals("type")) {
                String name = memberType.getElementsByClass("name").text();
                QualifiedName qualifiedName = new QualifiedName(className.getKey(),
                        (className.getValue().isEmpty() ? "" : (className.getValue() + "$")) + name);

                String[] declParts = memberType.text().split("\\s+=\\s+");

                initClass(qualifiedName);
                TypeAlias alias = new TypeAlias(name, Language.SCALA, getDoc(memberType),
                        getLink(memberType.nextElementSibling()), memberType.text());
                aliases.put(qualifiedName, alias);
                String attrString = memberType.getElementsByClass("modifier").get(0).text().trim();
                for (Map.Entry<String, String> attr : parseAttributes(attrString, ALIAS_ATTRIBUTES).entrySet()) {
                    alias.setAttr(attr.getKey(), attr.getValue());
                }

                ScalaType aliasedType = declParts.length == 2
                        ? ScalaType.parse(memberType, declParts[declParts.length - 1])
                        : new ScalaWildcard();
                aliasedTypes.put(qualifiedName, aliasedType);

                enclosingClasses.put(qualifiedName, className);

                declParts = typeSplit(typeSplit(declParts[0], "<:").get(0), ">:").get(0).split("\\s+" + Pattern.quote(name));
                if (declParts.length > 1) {
                    parseTypeParameters(memberType, classParameters.get(qualifiedName)
                            , classParametersConstraints.get(qualifiedName), declParts[declParts.length - 1]);
                }
            }
        }
    }

    private List<MemberEntity> parseMembers(Document document, QualifiedName enclosingClass, String className)
            throws IOException {
        List<MemberEntity> result = new ArrayList<>();
        for (Element membersSection : document.getElementsByClass("values")) {
            parseMembers(membersSection, enclosingClass, result);
        }

        parseConstructors(document.getElementById("constructors"), enclosingClass, className, result);

        return result;
    }

    private void parseMembers(Element members, QualifiedName enclosingClass, List<MemberEntity> result) throws IOException {
        for (Element member : members.getElementsByClass("signature")) {
            if (member.text().equals("def ^(x:")) {  // broken element in html doc
                continue;
            }

            String kind = member.getElementsByClass("kind").get(0).text();
            if (kind.equals("def") || kind.equals("val")) {
                parseClassMember(member, enclosingClass, result);
            } else if (kind.equals("object") || kind.equals("package")) {
                Element nameElem = member.getElementsByClass("name").get(0);
                String link = nameElem.parent().attr("href");
                String oldAddress = currentAddress;
                currentAddress = getNewAddress(link);
                if (kind.equals("object")) {
                    parseClass(enclosingClass);
                } else {
                    parsePackage();
                }
                currentAddress = oldAddress;
            }
        }
    }

    private void parseClassMember(Element methodElem, QualifiedName className, List<MemberEntity> result)
            throws MalformedURLException {
        String signature = methodElem.text().replaceAll("\\s+\\{.*\\}$", "");
        String addressName = signature;
        if (classMembers.get(className).containsKey(addressName)) {
            return;
        }
        Element nameElem = methodElem.getElementsByClass("name").isEmpty()
                ? (methodElem.getElementsByClass("implicit").isEmpty() ? null : methodElem.getElementsByClass("implicit").get(0))
                : methodElem.getElementsByClass("name").get(0);
        if (nameElem == null) {
            return;
        }

        String name = nameElem.text();
        String[] parts = signature.split("(def|val)\\s+" + Pattern.quote(name));
        signature = parts[1];
        String paramsString = "";
        if (signature.startsWith("[")) {
            int braces = 1;
            int i = 1;
            for (; braces > 0; ++i) {
                if (signature.charAt(i) == '[') {
                    braces++;
                } else if (signature.charAt(i) == ']') {
                    braces--;
                }
            }
            paramsString = signature.substring(0, i);
        }

        signature = signature.substring(paramsString.length(), signature.length());
        List<String> signatureParts = typeSplit(signature, ":");
        signature = "";
        String paramsPart = signatureParts.get(0);
        if (paramsPart.startsWith("(")) {
            List<String> paramsSections = splitByBraces(paramsPart, Arrays.asList('(', '['), Arrays.asList(')', ']'));
            signature = "(";
            for (String paramsSection : paramsSections) {
                List<String> funcParams = typeSplit(paramsSection.substring(1, paramsSection.length() - 1), ",");
                for (String param : funcParams) {
                    param = param.split("\\s=\\s")[0];
                    if (!param.startsWith("implicit")) {
                        signature += param.substring(param.indexOf(':') + 2, param.length()) + ",";
                    }
                }
            }
            if (signature.endsWith(",")) {
                signature = signature.substring(0, signature.length() - 1);
            }
            signature += ")";
        }
        String resultPart = StringUtil.join(signatureParts.subList(1, signatureParts.size()), ":");
        if (resultPart.contains("\u21D2")) {
            resultPart = "(" + resultPart + ")";
        }
        signature += " \u21D2 " + resultPart;

        memberTypes.get(className).put(addressName, ScalaFunction.parse(methodElem, signature));
        memberParameters.get(className).put(addressName, new ArrayList<String>());
        memberParametersConstraints.get(className).put(addressName, new ArrayList<ScalaConstraint>());

        if (!paramsString.isEmpty()) {
            parseTypeParameters(methodElem, memberParameters.get(className).get(addressName)
                    , memberParametersConstraints.get(className).get(addressName), paramsString);
        }

        MemberEntity function = new MemberEntity(name, Language.SCALA, getDoc(methodElem),
                getLink(methodElem.nextElementSibling()),
                methodElem.text());
        result.add(function);
        classMembers.get(className).put(addressName, function);
        String memberType = methodElem.getElementsByClass("kind").get(0).text().equals("def") ? "function" : "field";
        function.setAttr("memberType", memberType);
        String attrString = methodElem.getElementsByClass("modifier").get(0).text().trim();
        for (Map.Entry<String, String> attr : parseAttributes(attrString, MEMBER_ATTRIBUTES).entrySet()) {
            function.setAttr(attr.getKey(), attr.getValue());
        }
    }

    private void parseConstructors(Element constructors, QualifiedName enclosingClass, String className,
            List<MemberEntity> result) throws MalformedURLException {
        if (constructors == null) {
            return;
        }

        for (Element constructorElem : constructors.getElementsByClass("signature")) {
            String signature = constructorElem.text();
            String addressName = signature;
            String args = typeSplit(signature, className).get(1);

            String paramsString = "";
            if (args.startsWith("[")) {
                int braces = 1;
                int i = 1;
                for (; braces > 0; ++i) {
                    if (args.charAt(i) == '[') {
                        braces++;
                    } else if (args.charAt(i) == ']') {
                        braces--;
                    }
                }
                paramsString = args.substring(0, i);
            }
            args = args.substring((paramsString.isEmpty() ? 0 : paramsString.length()), args.length());

            signature = "(";
            for (String argSection : splitByBraces(args, Arrays.asList('(', '['), Arrays.asList(')', ']'))) {
                argSection = argSection.substring(1, argSection.length() - 1);
                for (String param : typeSplit(argSection, ",")) {
                    param = param.split("\\s=\\s")[0];
                    if (!param.startsWith("implicit")) {
                        signature += param.substring(param.indexOf(':') + 2, param.length()) + ",";
                    }
                }
            }
            if (signature.endsWith(",")) {
                signature = signature.substring(0, signature.length() - 1);
            }
            signature += ") \u21D2 " + className;

            memberTypes.get(enclosingClass).put(addressName, ScalaFunction.parse(constructorElem, signature));
            memberParameters.get(enclosingClass).put(addressName, new ArrayList<String>());
            memberParametersConstraints.get(enclosingClass).put(addressName, new ArrayList<ScalaConstraint>());

            if (!paramsString.isEmpty()) {
                parseTypeParameters(constructorElem, memberParameters.get(enclosingClass).get(addressName)
                        , memberParametersConstraints.get(enclosingClass).get(addressName), paramsString);
            }

            MemberEntity constructor = new MemberEntity(className, Language.SCALA, getDoc(constructorElem),
                    getLink(constructorElem.nextElementSibling()), constructorElem.text());
            result.add(constructor);
            classMembers.get(enclosingClass).put(addressName, constructor);
            constructor.setAttr("memberType", "constructor");
            String attrString = constructorElem.getElementsByClass("modifier").get(0).text().trim();
            for (Map.Entry<String, String> attr : parseAttributes(attrString, MEMBER_ATTRIBUTES).entrySet()) {
                constructor.setAttr(attr.getKey(), attr.getValue());
            }
        }
    }

    private String getDoc(Element entityElem) {
        if (entityElem.nextElementSibling() == null) {
            return "";
        }

        Element docElem = entityElem.nextElementSibling().nextElementSibling();
        if (docElem == null) {
            return "";
        }

        if (docElem.hasClass("shortcomment") && docElem.nextElementSibling() != null
                && docElem.nextElementSibling().hasClass("fullcomment")) {
            docElem = docElem.nextElementSibling();
        }

        return (docElem.hasClass("shortcomment") || docElem.hasClass("fullcomment")) ? docElem.text() : "";
    }

    private void initClass(QualifiedName className) {
        classParameters.put(className, new ArrayList<String>());
        classParametersConstraints.put(className, new ArrayList<ScalaConstraint>());
        memberTypes.put(className, new HashMap<String, ScalaType>());
        memberParameters.put(className, new HashMap<String, List<String>>());
        memberParametersConstraints.put(className, new HashMap<String, List<ScalaConstraint>>());
        classMembers.put(className, new HashMap<String, MemberEntity>());
        superTypes.put(className, new ArrayList<ScalaType>());
    }

    private void fillOther() {
        PackageEntity otherPackage = new PackageEntity(OTHER_PACKAGE, Language.SCALA, new ArrayList<TypeConstructor>(),
                new ArrayList<MemberEntity>(), new ArrayList<PackageEntity>(), "", null);
        packages.put(OTHER_PACKAGE, otherPackage);

        QualifiedName name = new QualifiedName(OTHER_PACKAGE, "*");
        initClass(name);
        enclosingClasses.put(name, new QualifiedName(OTHER_PACKAGE, ""));

        Classifier star = new Classifier("*", Language.SCALA, "", null, new ArrayList<MemberEntity>(), "*");
        classes.put(name, star);
    }

    static List<String> typeSplit(String signature, String str) {
        return ParserUtils.typeSplit(signature, str, Arrays.asList('[', '('), Arrays.asList(']', ')'));
    }

    static String getPackageName(Element elem, String className) {
        Elements linkElems = elem.getElementsMatchingOwnText("^" + Pattern.quote(className) + "$");
        if (linkElems.isEmpty() || !linkElems.get(linkElems.size() - 1).tagName().equals("a")) {
            return null;
        }

        String fullName = linkElems.get(linkElems.size() - 1).attr("name");
        return fullName.substring(0, fullName.lastIndexOf('.'));
    }

    private URL getLink(Element urlElem) throws MalformedURLException {
        if (urlElem == null) {
            return null;
        }

        String link = urlElem.getElementsByAttributeValue("title", "Permalink").get(0).attr("href").replaceAll("\\.\\./", "");
        return new URL(BASE_ADDRESS + link);
    }

    private String getNewAddress(String link) {
        int parents = 0;
        while (link.startsWith("../")) {
            link = link.substring(3, link.length());
            parents++;
        }
        String[] addressParts = currentAddress.substring(0, currentAddress.lastIndexOf('/')).split("/");
        String result = "";
        for (int i = 0; i < addressParts.length - parents; ++i) {
            result += addressParts[i] + "/";
        }
        return result + link;
    }

    public Map<String, PackageEntity> getPackages() {
        return packages;
    }

    public Map<QualifiedName, Classifier> getClasses() {
        return classes;
    }

    public Map<QualifiedName, TypeAlias> getAliases() {
        return aliases;
    }
    public Map<QualifiedName, Map<String, MemberEntity>> getClassMembers() {
        return classMembers;
    }
}
