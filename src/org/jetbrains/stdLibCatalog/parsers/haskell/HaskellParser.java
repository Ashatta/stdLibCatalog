package org.jetbrains.stdLibCatalog.parsers.haskell;

import org.jetbrains.stdLibCatalog.domain.*;
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

/**
 * TODO: split functions into functional groups
 * todo: data and newtype fields
 */
public class HaskellParser {

    static final String BASE_ADDRESS = "http://www.haskell.org/ghc/docs/latest/html/libraries/";
    static final int CONNECTION_TIMEOUT = 2000;
    static final String OTHER_PACKAGE = "other";  // a name of dummy package for embedded stuff like lists or tuples

    private String packageName = "";
    private String currentAddress = "";
    private final Map<String, Element> shortDefinitions = new HashMap<>();
    private final Map<String, PackageEntity> packages = new HashMap<>();
    private final Map<String, String> packageDoc = new HashMap<>();

    private final Map<QualifiedName, MemberEntity> functions = new HashMap<>();
    private final Map<QualifiedName, Classifier> classes = new HashMap<>();
    private final Map<QualifiedName, TypeAlias> aliases = new HashMap<>();
    private final Map<QualifiedName, List<QualifiedName>> parents = new HashMap<>();
    private final Map<QualifiedName, List<QualifiedName>> derived = new HashMap<>();
    private final Map<QualifiedName, HaskellType> signatures = new HashMap<>();
    private final List<Element> instances = new ArrayList<>();
    private final Map<QualifiedName, List<HaskellConstraint>> constraints = new HashMap<>();
    private final Map<QualifiedName, List<TypeVariable>> entityParams = new HashMap<>();

    public static void main(String[] args) throws IOException {
        HaskellParser parser = new HaskellParser();
        parser.parse();
/*
        File dir = new File("resources/tests/parsers/haskell/global");
        if (dir.exists()) {
            dir.delete();
        }
        dir.mkdir();
        for (PackageEntity pack : parser.packages.values()) {
            FileWriter out = new FileWriter(dir.getAbsolutePath() + "/" + pack.getName() + ".txt");
            out.write(pack.toString());
            out.close();
        }
*/
    }

    public void parse() throws IOException {
        fillListAndTuples(); //todo dph тут же еще нет ничего?

        Document moduleList = Jsoup.parse(new URL(BASE_ADDRESS), CONNECTION_TIMEOUT);
        Elements modules = moduleList.getElementById("module-list").getElementsByTag("ul").get(0).children();
        for (Element module : modules) {
            parseModule(module);
        }

        createEntitiesConnections();
    }

    private PackageEntity parseModule(Element module) throws IOException {
        // there are strange non-breaking spaces before module name sometimes
        String curr = module.child(0).text().replaceAll("^\\p{javaSpaceChar}", ""); //todo dph А не проще сделат trim?
        String link = "";

        List<PackageEntity> subpackages = parseSubpackages(module);

        Elements moduleLinkElem = module.child(0).getElementsByAttribute("href");
        if (!moduleLinkElem.isEmpty()) {
            packageName = curr;
            link = moduleLinkElem.get(0).attributes().get("href");
            currentAddress = BASE_ADDRESS + link;
            parse(new URL(currentAddress));
        }

        PackageEntity pack = new PackageEntity(curr, Language.HASKELL, classesByPackageName(curr),
                functionsByPackageName(curr), subpackages, (packageDoc.containsKey(curr) ? packageDoc.get(curr) : ""),
                link.equals("") ? null : new URL(BASE_ADDRESS + link));
        packages.put(curr, pack);

        for (PackageEntity subpackage : subpackages) {
            subpackage.setContainingPackage(pack);
        }

        return pack;
    }

    private List<PackageEntity> parseSubpackages(Element module) throws IOException {
        List<PackageEntity> subpackages = new ArrayList<>();

        Elements lists = module.getElementsByTag("ul");
        if (!lists.isEmpty()) {
            for (Element subModule : lists.get(0).children()) {
                PackageEntity sub = parseModule(subModule);
                if (sub != null) {
                    subpackages.add(sub);
                }
            }
        }

        return subpackages;
    }

    public void parse(URL url) throws IOException {
        Document document = Jsoup.parse(url, CONNECTION_TIMEOUT);
        packageDoc.put(packageName, getDoc(document.getElementById("description")));

        if (packageName.equals("Compiler.Hoopl.Wrappers")) {
            return;
        }

        shortDefinitions.put(packageName, document.getElementById("section.syn"));
        if (shortDefinitions.get(packageName) == null) {
            shortDefinitions.put(packageName, document.getElementById("interface"));
        }

        for (Element elem : document.getElementsByClass("top")) {
            parseElem(elem);
        }
    }

    private void parseElem(Element elem) throws MalformedURLException {
        if (isTypeClass(elem)) {
            parseTypeClass(elem);
        } else if (isDataType(elem)) {
            parseData(elem);
        } else if (isTypeAlias(elem)) {
            parseTypeAlias(elem);
        } else if (isFunction(elem)) {
            parseFunctionDef(elem.child(0));
        }
    }

    private boolean isFunction(Element elem) {
        return !isTypeClass(elem) && !isDataType(elem) && !isTypeAlias(elem)
                && elem.getElementsByClass("src").get(0).getElementsMatchingOwnText("^module$").isEmpty()
                && elem.getElementsByClass("src").get(0).getElementsMatchingOwnText("^type family$").isEmpty();
    }

    private static boolean isTypeClass(Element elem) {
        return !elem.getElementsByClass("src").get(0).getElementsMatchingOwnText("^class$").isEmpty();
    }

    private static boolean isDataType(Element elem) {
        return !elem.getElementsByClass("src").get(0).getElementsMatchingOwnText("^data$").isEmpty()
                || !elem.getElementsByClass("src").get(0).getElementsMatchingOwnText("^newtype$").isEmpty();
    }

    private static boolean isTypeAlias(Element elem) {
        return !elem.getElementsByClass("src").get(0).getElementsMatchingOwnText("^type$").isEmpty()
                && elem.getElementsByClass("src").get(0).getElementsMatchingOwnText("^type family$").isEmpty();
    }

    private void parseTypeClass(Element elem) throws MalformedURLException {
        String name = elem.child(0).getElementsByClass("def").get(0).text();
        QualifiedName qualifiedName = new QualifiedName(packageName, name);

        parents.put(qualifiedName, parseTypeClassParents(elem.children().get(0), name));
        parseInstances(elem);

        addConstraints(qualifiedName, new ArrayList<HaskellConstraint>());
        entityParams.put(qualifiedName, new ArrayList<TypeVariable>());

        Classifier typeClass = new Classifier(name, Language.HASKELL, getDoc(elem),
                new URL(currentAddress + "#t:" + name), parseFunctions(elem), getTypeClassDefinition(name));
        typeClass.setAttr("classifierType", "typeclass");
        typeClass.setAttr("fakeInstanceClassifier", "false");

        classes.put(qualifiedName, typeClass);
    }

    private String getTypeClassDefinition(String name) {
        Element shortDef = shortDefinitions.get(packageName);

        Element elem = null;
        for (Element el : shortDef.getElementsMatchingText("^class\\s+(|.*=>\\s+)" + Pattern.quote(name))) {
            if (shortDef.id().equals("interface")) {
                el = el.parent();
            }
            String[] def = el.text().split("where")[0].split("=>");
            if (def[def.length - 1].contains(" " + name + " ")) {
                elem = el;
            }
        }

        if (shortDef.id().equals("section.syn")) {
            return elem.text();
        } else {
            elem = elem.parent();
            String definition = elem.getElementsByClass("src").get(0).text().split("\\s+Source(\\s|$)")[0];
            if (!elem.getElementsByClass("methods").isEmpty()) {
                Elements methods = elem.getElementsByClass("methods").get(0).getElementsByClass("src");
                for (Element method : methods) {
                    definition += "\n\t" + method.text().split("\\s+Source(\\s|$)")[0];
                }
            }

            return definition;
        }
    }
    private List<QualifiedName> parseTypeClassParents(Element element, String parentName) {
        List<QualifiedName> result = new ArrayList<>();

        for (Element parentLink : element.getElementsByAttribute("href")) {
            if (parentLink.hasClass("link")) {
                continue;
            }

            String parentPackage = getPackageName(parentLink.attr("href"));
            QualifiedName parent = new QualifiedName(parentPackage, parentLink.text());
            result.add(parent);
             if (!derived.containsKey(parent)) {
                 derived.put(parent, new ArrayList<QualifiedName>());
            }
            derived.get(parent).add(new QualifiedName(packageName, parentName));
        }

        return result;
    }

    static String getPackageName(String link) {
        // link has format "http://something/something/Package-Name.html", getting "Package.Name" out of it
        String[] parts = link.split("\\.html")[0].split("/");
        return parts[parts.length - 1].replaceAll("-", ".");
    }

    private void parseInstances(Element elem) {
        if (elem.getElementsByClass("instances").isEmpty()) {
            return;
        }

        for (Element el : elem.getElementsByClass("instances").get(0).getElementsByClass("src")) {
            instances.add(el);
        }
    }

    private List<MemberEntity> parseFunctions(Element elem) throws MalformedURLException {
        List<MemberEntity> functions = new ArrayList<>();

        Elements methodsElem = elem.getElementsByClass("methods");
        if (!methodsElem.isEmpty()) {
            for (Element func : methodsElem.get(0).getElementsByClass("src")) {
                if (!func.getElementsByClass("def").isEmpty()) {
                    functions.addAll(parseFunctionDef(func));
                }
            }
        }

        return functions;
    }

    private List<MemberEntity> parseFunctionDef(Element func) throws MalformedURLException {
        // names of functions in the definition (which can contain more than one function, divided by ',')
        String[] names = HaskellType.typeSplit(func.child(0).text(), "::").get(0).split(",\\s+");

        List<MemberEntity> result = new ArrayList<>();
        for (String name : names) {
            QualifiedName funcName = new QualifiedName(packageName, name);
            if (!functions.containsKey(funcName)) {        // module Ix - rangeSize definition is repeated twice
                result.add(parseFunction(func, funcName));
            }
        }

        return result;
    }

    private MemberEntity parseFunction(Element func, QualifiedName name) throws MalformedURLException {
        Element funcShortDef = shortDefinitions.get(packageName).getElementsMatchingText(
                "^" + Pattern.quote(name.getValue()) + "$").last().parent();
        String signature = getSignature(funcShortDef);
        parseParameters(name, signature, true);
        fillFunctionType(funcShortDef, name, signature);

        Element doc = func.nextElementSibling();
        String d = "";
        if (doc != null) {
            d = doc.text();
        }

        String definition = shortDefinitions.get(packageName).getElementsMatchingText(
                "^" + Pattern.quote(name.getValue()) + "(\\s|,)").last().text().split("\\s+Source(\\s|$)")[0];

        MemberEntity function = new MemberEntity(name.getValue(), Language.HASKELL, d,
                new URL(currentAddress + "#v:" + name.getValue()), definition);
        function.setAttr("memberType", "function");

        functions.put(name, function);
        return function;
    }

    private void fillFunctionType(Element func, QualifiedName name, String signature) {
        List<HaskellConstraint> constraints = new ArrayList<>();
        parseSignature(func, signature, name, constraints);
        addConstraints(name, constraints);
    }

    private void parseParameters(QualifiedName name, String signature, boolean isFunction) {
        if (!entityParams.containsKey(name)) {
            entityParams.put(name, new ArrayList<TypeVariable>());
        }

        List<String> paramNames = new ArrayList<>();
        boolean first = true;
        for (String s : signature.split("\\s+")) {
            s = s.replaceAll("[\\(\\)\\[\\],\\->#]", "");  // keep parameters without list-tuple-function artifacts
            if (!s.isEmpty() && Character.isLowerCase(s.charAt(0)) && (isFunction || !first || s.charAt(0) != 'k')
                    && !paramNames.contains(s)) {
                paramNames.add(s);
                first = false;
            }
        }

        for (String param : paramNames) {
            entityParams.get(name).add(new TypeVariable(param, Language.HASKELL));
        }
    }

    private void parseSignature(Element el, String signature, QualifiedName name, List<HaskellConstraint> constraints) {
        signature = "(" + signature + ")";
        signatures.put(name, HaskellType.parse(el, signature, constraints));
    }

    private String getSignature(Element func) {
        List<String> parts = HaskellType.typeSplit(func.text(), "::");  // split into name and type description

        for (Element elem : func.getElementsByClass("fixity")) {
            parts.set(1, parts.get(1).replaceAll(elem.text(), ""));
        }

        for (Element elem : func.getElementsByClass("rightedge")) {
            parts.set(1, parts.get(1).replaceAll(elem.text(), ""));
        }

        for (Element elem : func.getElementsByClass("link")) {
            parts.set(1, parts.get(1).replaceAll(elem.text(), ""));
        }

        // ignoring forall specifications for simplicity of further parsing
        return parts.get(1).replaceAll("forall(\\s+[a-z']+)*\\.\\s+", "").trim();
    }

    private void parseData(Element elem) throws MalformedURLException {
        String name = elem.child(0).getElementsByClass("def").get(0).text();
        if (isInfix(name)) {
            name = "(" + name + ")";
        }

        // removing links to source, keywords and kind specification
        String def = elem.children().get(0).text().split("\\s+Source(\\s|$)")[0].split("\\s+where")[0]
                .split("\\s+infix")[0].split("\\s+::\\s+")[0]
                .replaceFirst("\\s*data\\s+", "").replaceFirst("\\s*newtype\\s+", "");

        parseTypeParameters(elem, def, name);
        parseInstances(elem);

        Classifier data = new Classifier(name, Language.HASKELL, getDoc(elem),
                new URL(currentAddress + "#t:" + name), new ArrayList<MemberEntity>(), getDataDefinition(def));
        data.setAttr("classifierType", data.getDefinition().startsWith("data") ? "data" : "newtype");
        data.setAttr("fakeInstanceClassifier", "false");
        data.setAttr("infix", (name.length() > 2 && isInfix(name.substring(1, name.length() - 1))) ? "true" : "false");

        classes.put(new QualifiedName(packageName, name), data);
    }

    private String getDataDefinition(String name) {
        Element shortDef = shortDefinitions.get(packageName);
        Element elem = shortDef.getElementsMatchingText("^(data|newtype)\\s+" + Pattern.quote(name)).last();
        if (shortDef.id().equals("section.syn")) {
            return elem.text();
        } else {
            elem = elem.parent();
            String definition = elem.getElementsByClass("src").get(0).text().split("\\s+Source(\\s|$)")[0] + " = ";
            if (!elem.getElementsByClass("constructors").isEmpty()) {
                Elements constructors = elem.getElementsByClass("constructors").get(0).getElementsByTag("tr");
                for (Element constructor : constructors) {
                    Elements fieldsElem = constructor.child(0).getElementsByClass("fields");
                    if (!fieldsElem.isEmpty()) {
                        definition = definition.substring(0, definition.length() - 3) + " { ";
                        Elements fields = fieldsElem.get(0).getElementsByTag("dt");
                        for (Element field : fields) {
                            definition += "\n\t" + field.text().split("\\s+Source(\\s|$)")[0];
                        }
                        definition += "\n} ";
                    } else {
                        definition += constructor.child(0).text().split("\\s+Source(\\s|$)")[0];
                    }
                    definition += " | ";
                }
            }

            return definition.substring(0, definition.length() - 3);
        }
    }

    private void parseTypeParameters(Element elem, String def, String name) {
        String[] parts = def.split("\\s+=>\\s+");
        String[] params = parts[parts.length - 1].split("\\s+");

        QualifiedName qualifiedName = new QualifiedName(packageName, name);
        if (!entityParams.containsKey(qualifiedName)) {
            entityParams.put(qualifiedName, new ArrayList<TypeVariable>());
        }

        for (String param : params) {
            param = param.replaceAll("[\\(\\)\\[\\],\\->#]", "");
            if (!param.isEmpty() && Character.isLowerCase(param.charAt(0))) {
                entityParams.get(qualifiedName).add(new TypeVariable(param, Language.HASKELL));
            }
        }

        List<HaskellConstraint> constraints = new ArrayList<>();
        HaskellConstraint.parseConstraints(elem, parts.length > 1 ? parts[0] : "", constraints);
        addConstraints(qualifiedName, constraints);
    }

    private void parseTypeAlias(Element elem) throws MalformedURLException {
        String name = elem.child(0).getElementsByClass("def").get(0).text();
        QualifiedName qualifiedName = new QualifiedName(packageName, name);

        Element def = shortDefinitions.get(packageName).getElementsMatchingText(
                "^type\\s+" + Pattern.quote(name)).last();
        String strDef = def.text();
        for (Element linkElem : def.getElementsByClass("link")) {
            strDef = strDef.replaceAll(linkElem.text(), "");
        }
        strDef = strDef.replaceAll("forall(\\s+[a-z]+)*\\.\\s+", "").trim();

        String[] parts = strDef.split("\\s+=\\s+");
        parts[0] = parts[0].replaceAll("^type\\s+", "").trim();
        parts[1] = parts[1].trim();

        parseParameters(qualifiedName, parts[1], false);
        List<HaskellConstraint> constraints = new ArrayList<>();
        parseSignature(elem, parts[1], qualifiedName, constraints);
        addConstraints(qualifiedName, constraints);

        aliases.put(qualifiedName, new TypeAlias(name, Language.HASKELL, getDoc(elem),
                new URL(currentAddress + "#t:" + name), def.text()));
    }

    private static String getDoc(Element elem) {
        String doc = "";

        if (elem != null) {
            Elements docs = elem.getElementsByClass("doc");
            if (!docs.isEmpty()) {
                doc = docs.get(0).text();
            }
        }

        return doc;
    }

    private void createEntitiesConnections() {
        for (Map.Entry<QualifiedName, Classifier> classDesc : classes.entrySet()) {
            classDesc.getValue().setContainingPackage(packages.get(classDesc.getKey().getKey()));
            createClassifierConnections(classDesc.getValue());
        }

        for (Map.Entry<QualifiedName, TypeAlias> alias : aliases.entrySet()) {
            alias.getValue().setContainingPackage(packages.get(alias.getKey().getKey()));
            createAliasConnections(alias.getValue());
        }

        for (Map.Entry<QualifiedName, MemberEntity> func : functions.entrySet()) {
            func.getValue().setContainingPackage(packages.get(func.getKey().getKey()));
            createFunctionConnections(func.getValue());
        }

        for (Element instance : instances) {
            parseInstance(instance);
        }
    }

    private void createClassifierConnections(Classifier classifier) {
        String packName = classifier.getContainingPackage().getName();

        connectDerived(classifier, packName, true);
        connectDerived(classifier, packName, false);
        connectTypeParameters(classifier, packName);

        for (MemberEntity function : classifier.getMembers()) {
            function.setContainingType(classifier);
        }
    }

    private void connectDerived(Classifier classifier, String packName, boolean isParent) {
        Map<QualifiedName, List<QualifiedName>> connected = isParent ? derived : parents;
        QualifiedName classifierName = new QualifiedName(packName, classifier.getName());
        if (connected.containsKey(classifierName)) {
            for (QualifiedName connection : connected.get(classifierName)) {
                if (classes.containsKey(connection)) {
                    if (isParent) {
                        classifier.addDerived(classes.get(connection));
                    } else {
                        classifier.addBase(new DataType(classes.get(connection), new ArrayList<Type>()));
                    }
                }
            }
        }
    }

    // TODO: next two methods, copypaste
    private void connectTypeParameters(Classifier classEntity, String packName) {
        QualifiedName name = new QualifiedName(packName, classEntity.getName());

        List<Constraint> endConstraints = new ArrayList<>();
        for (HaskellConstraint constraint : constraints.get(name)) {
            endConstraints.add(constraint.buildConstraint(this, entityParams.get(name)));
        }

        for (TypeVariable variable : entityParams.get(name)) {
            for (Constraint constraint : endConstraints) {
                if (constraint.getVariables().contains(variable)) {
                    variable.addConstraint(constraint);
                }
            }
            classEntity.addParameter(variable);
        }
    }

    private void createFunctionConnections(MemberEntity function) {
        QualifiedName funcName = new QualifiedName(function.getContainingPackage().getName(), function.getName());

        List<Constraint> endConstraints = new ArrayList<>();
        for (HaskellConstraint constraint : constraints.get(funcName)) {
            endConstraints.add(constraint.buildConstraint(this, entityParams.get(funcName)));
        }
        addTypeClassConstraint(function, endConstraints);

        for (TypeVariable variable : entityParams.get(funcName)) {
            for (Constraint constraint : endConstraints) {
                if (constraint.getVariables().contains(variable)) {
                    variable.addConstraint(constraint);
                }
            }
            function.addParameter(variable);
        }

        function.setSignature(signatures.get(funcName).makeSignature(this, funcName, false));
    }

    private void addTypeClassConstraint(MemberEntity function, List<Constraint> endConstraints) {
        if (function.getContainingType() != null) {
            Constraint constraint = typeClassConstraint(function);
            boolean add = true;
            for (Constraint constr : endConstraints) {
                if (constr.getDeclaration().equals(constraint.getDeclaration())) {
                    add = false;
                    break;
                }
            }

            if (add) {
                endConstraints.add(constraint);
            }
        }
    }

    private Constraint typeClassConstraint(MemberEntity function) {
        Classifier typeClass = function.getContainingType();
        List<Entity> typeClassList = new ArrayList<>();
        typeClassList.add(function.getContainingType());

        String[] def = typeClass.getDefinition().split("\\s+where")[0].split(Pattern.quote(typeClass.getName()) + "\\s+");
        String[] vars = def[def.length - 1].split("\\s+");
        List<TypeVariable> variables = new ArrayList<>();
        for (TypeVariable variable : entityParams.get(
                new QualifiedName(function.getContainingPackage().getName(), function.getName()))) {
            for (String var : vars) {
                if (var.equals(variable.getName())) {
                    variables.add(variable);
                }
            }
        }

        return new Constraint(variables, typeClassList,
                typeClass.getName() + " " + StringUtil.join(Arrays.asList(vars), " "));
    }

    private void createAliasConnections(TypeAlias alias) {
        QualifiedName name = new QualifiedName(alias.getContainingPackage().getName(), alias.getName());

        List<Constraint> endConstraints = new ArrayList<>();
        for (HaskellConstraint constraint : constraints.get(name)) {
            endConstraints.add(constraint.buildConstraint(this, entityParams.get(name)));
        }

        for (TypeVariable variable : entityParams.get(name)) {
            for (Constraint constraint : endConstraints) {
                if (constraint.getVariables().contains(variable)) {
                    variable.addConstraint(constraint);
                }
            }
            alias.addParameter(variable);
        }

        alias.setAliasedType(signatures.get(name).buildType(this, name, true));
    }

    private void parseInstance(Element el) {
        String[] decl = el.text().split("\\s+=>\\s+");
        if (decl[decl.length - 1].startsWith("type")) {
            return;
        }
        String fullType = decl[decl.length - 1].replaceAll("type\\s+", "");

        QualifiedName typeClass = getTypeClass(el, fullType);
        fullType = removeKinds(fullType.replaceAll(Pattern.quote(typeClass.getValue()) + "\\s+", ""));

        List<HaskellConstraint> constraints = new ArrayList<>();
        HaskellConstraint.parseConstraints(el, decl.length > 1 ? decl[0] : "", constraints);
        HaskellType instanceType = HaskellType.parse(el, fullType, constraints);

        QualifiedName entityQualifiedName = getEntityName(el, instanceType.getName());
        Classifier fakeClassifier = instanceType.buildClassifier(decl[decl.length - 1], typeClass,
                classes.containsKey(entityQualifiedName) ? classes.get(entityQualifiedName).getParameters().size() : 0);

        fakeClassifier.setAttr("classifierType", classes.containsKey(entityQualifiedName)
                ? classes.get(entityQualifiedName).getAttr("classifierType")
                : "data");
        fakeClassifier.setAttr("fakeInstanceClassifier", "true");
        fakeClassifier.setAttr("isInfix", instanceType.classifierName().startsWith("(") ? "true" : "false");

        fillConstraints(fakeClassifier, constraints);
        fakeClassifier.setContainingPackage(packages.get(entityQualifiedName.getKey())); // todo maybe separate fake package for them?

        QualifiedName fakeClassifierName = new QualifiedName(fakeClassifier.getContainingPackage().getName(),
                fakeClassifier.getName());
        if (classes.containsKey(fakeClassifierName)) {
            return;
        }

        fakeClassifier.addBase(new DataType(classes.get(typeClass), new ArrayList<Type>()));
        classes.get(typeClass).addDerived(fakeClassifier);

        classes.put(fakeClassifierName, fakeClassifier);
    }

    private QualifiedName getTypeClass(Element el, String fullType) {
        String typeClassName = fullType.split("\\s+")[0];
        Elements typeClassLinks = el.getElementsMatchingOwnText("^" + Pattern.quote(typeClassName) + "$");
        String typeClassModule = !typeClassLinks.isEmpty() && typeClassLinks.last().hasAttr("href")
                ? getPackageName(typeClassLinks.last().attr("href"))
                : "Data." + typeClassName;
        return new QualifiedName(typeClassModule, typeClassName);
    }

    private QualifiedName getEntityName(Element el, String name) {
        QualifiedName entityQualifiedName;
        if (name.equals("List")) {
            entityQualifiedName = new QualifiedName(OTHER_PACKAGE, name);
        } else if (name.matches("^\\(,*\\)$")) {
            entityQualifiedName = new QualifiedName("GHC.Tuple", name);
        } else {
            Elements linksToEntity = el.getElementsMatchingOwnText("^" + Pattern.quote(name) + "$");
            if (!linksToEntity.isEmpty() && linksToEntity.last().text().equals(name)) {
                entityQualifiedName = new QualifiedName(getPackageName(linksToEntity.last().attr("href")), name);
            } else {
                entityQualifiedName = new QualifiedName(OTHER_PACKAGE, name);
            }
        }

        return entityQualifiedName;
    }

    private String removeKinds(String type) {
        int i = 0;
        int braces = 0;
        while (type.charAt(i) == '(') {
            ++i;
            ++braces;
        }

        if (type.charAt(i) == 'k' || type.charAt(i) == '*') {
            while (braces > 0 || !Character.isWhitespace(type.charAt(i))) {
                if (type.charAt(i) == '(') {
                    ++braces;
                } else if (type.charAt(i) == ')') {
                    --braces;
                }
                ++i;
            }
            while (Character.isWhitespace(type.charAt(i))) {
                ++i;
            }
            type = type.substring(i, type.length());
        }

        return type;
    }

    private void fillConstraints(Classifier fakeClassifier, List<HaskellConstraint> constraints) {
        List<Constraint> endConstraints = new ArrayList<>();
        for (HaskellConstraint constraint : constraints) {
            endConstraints.add(constraint.buildConstraint(this, fakeClassifier.getParameters()));
        }

        for (TypeVariable variable : fakeClassifier.getParameters()) {
            for (Constraint constraint : endConstraints) {
                if (constraint.getVariables().contains(variable)) {
                    variable.addConstraint(constraint);
                }
            }
        }
    }

    private List<TypeConstructor> classesByPackageName(String packName) {
        List<TypeConstructor> result = new ArrayList<>();
        for (Map.Entry<QualifiedName, Classifier> classifier : classes.entrySet()) {
            if (classifier.getKey().getKey().equals(packName)) {
                result.add(classifier.getValue());
            }
        }
        for (Map.Entry<QualifiedName, TypeAlias> alias : aliases.entrySet()) {
            if (alias.getKey().getKey().equals(packName)) {
                result.add(alias.getValue());
            }
        }
        return result;
    }

    private List<MemberEntity> functionsByPackageName(String packName) {
        List<MemberEntity> result = new ArrayList<>();
        for (Map.Entry<QualifiedName, MemberEntity> function : functions.entrySet()) {
            if (function.getKey().getKey().equals(packName)) {
                result.add(function.getValue());
            }
        }
        return result;
    }

    private void addConstraints(QualifiedName name, List<HaskellConstraint> constraints) {
        List<String> declarations = new ArrayList<>();
        this.constraints.put(name, new ArrayList<HaskellConstraint>());
        for (HaskellConstraint constraint : constraints) {
            if (!declarations.contains(constraint.getDeclaration())) {
                this.constraints.get(name).add(constraint);
                declarations.add(constraint.getDeclaration());
            }
        }
    }

    private void fillListAndTuples() {
        // TODO: docs for lists and "other" package
        packageDoc.put(OTHER_PACKAGE, "");
        List<TypeConstructor> otherClasses = new ArrayList<>();
        Classifier list = new Classifier("List", Language.HASKELL, "", null, new ArrayList<MemberEntity>(), "");
        list.setAttr("classifierType", "data");
        list.setAttr("fakeInstanceClassifier", "false");
        list.addParameter(new TypeVariable("a", Language.HASKELL));
        otherClasses.add(list);
        QualifiedName listQualified = new QualifiedName(OTHER_PACKAGE, "List");
        classes.put(listQualified, list);
        parents.put(listQualified, new ArrayList<QualifiedName>());
        addConstraints(listQualified, new ArrayList<HaskellConstraint>());
        entityParams.put(listQualified, new ArrayList<TypeVariable>());

        String name = "(" + new String(new char[63]).replace("\0", ",") + ")";
        Classifier tuple = new Classifier(name, Language.HASKELL, "", null, new ArrayList<MemberEntity>(), "");
        tuple.setAttr("classifierType", "data");
        tuple.setAttr("fakeInstanceClassifier", "false");
        for (int j = 0; j < 64; ++j) {
            String paramName = (j > 25 ? "a" : "") + String.valueOf((char) ('a' + (j % 26)));
            tuple.addParameter(new TypeVariable(paramName, Language.HASKELL));
        }
        otherClasses.add(tuple);
        QualifiedName qualifiedName = new QualifiedName(OTHER_PACKAGE, name);
        classes.put(qualifiedName, tuple);
        parents.put(qualifiedName, new ArrayList<QualifiedName>());
        addConstraints(qualifiedName, new ArrayList<HaskellConstraint>());
        entityParams.put(qualifiedName, new ArrayList<TypeVariable>());

        PackageEntity other = new PackageEntity(OTHER_PACKAGE, Language.HASKELL, otherClasses,
                new ArrayList<MemberEntity>(), new ArrayList<PackageEntity>(), "", null);
        for (TypeConstructor otherClass : otherClasses) {
            otherClass.setContainingPackage(other);
        }
        packages.put(OTHER_PACKAGE, other);
    }

    private static boolean isInfix(String name) {
        return name.equals(":~:") || name.equals("~") || name.equals(":+:") || name.equals(":*:") || name.equals(":.:");
    }

    public Map<String, Element> getShortDefinitions() {
        return shortDefinitions;
    }

    public Map<String, PackageEntity> getPackages() {
        return packages;
    }

    public Map<QualifiedName, MemberEntity> getFunctions() {
        return functions;
    }

    public Map<QualifiedName, Classifier> getClasses() {
        return classes;
    }

    public Map<QualifiedName, TypeAlias> getAliases() {
        return aliases;
    }

    public Map<QualifiedName, List<TypeVariable>> getEntityParams() {
        return entityParams;
    }
}