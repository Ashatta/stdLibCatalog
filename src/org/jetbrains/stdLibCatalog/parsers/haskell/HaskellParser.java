package org.jetbrains.stdLibCatalog.parsers.haskell;

import javafx.util.Pair;
import org.jetbrains.stdLibCatalog.domain.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 * TODO: tuple-like and infix constructors
 * TODO: links to source code
 * TODO: links to documentation
 * TODO: type synonyms
 * TODO: split functions into functional groups
 */
public class HaskellParser {
    static class QualifiedName extends Pair<String, String> {
        public QualifiedName(String packageName, String entityName) {
            super(packageName, entityName);
        }
    }

    static class ParameterDescription extends Pair<Integer, List<QualifiedName>> {
        public ParameterDescription(Integer parameterNumber, List<QualifiedName> constraintsDescription) {
            super(parameterNumber, constraintsDescription);
        }
    }

    static final String BASE_ADDRESS = "http://www.haskell.org/ghc/docs/latest/html/libraries/";
    static final int CONNECTION_TIMEOUT = 2000;
    static final String OTHER_PACKAGE = "other";  // a name of dummy package for embedded stuff like lists or tuples

    String packageName = "";
    final Map<String, Element> shortDefinitions = new HashMap<>();
    final Map<String, PackageEntity> packages = new HashMap<>();
    final Map<String, String> packageDoc = new HashMap<>();
    final Map<QualifiedName, FunctionEntity> functions = new HashMap<>();
    final Map<QualifiedName, Classifier> classes = new HashMap<>();
    final Map<QualifiedName, TypeAlias> aliases = new HashMap<>();
    final Map<QualifiedName, List<QualifiedName>> parents = new HashMap<>();
    final Map<QualifiedName, List<QualifiedName>> derived = new HashMap<>();
    final Map<QualifiedName, Map<String, ParameterDescription>> entityParameters = new HashMap<>();
    final Map<QualifiedName, Map<String, TypeVariable>> entityEndParameters = new HashMap<>();
    final Map<QualifiedName, HaskellType> signatures = new HashMap<>();

    public static void main(String[] args) throws IOException {
        HaskellParser parser = new HaskellParser();
        parser.fillListAndTuples();

        Document moduleList = Jsoup.parse(new URL(BASE_ADDRESS), CONNECTION_TIMEOUT);
        Elements modules = moduleList.getElementById("module-list").getElementsByTag("ul").get(0).children();
        for (Element module : modules) {
            parser.parseModule(module);
        }

        parser.createEntitiesConnections();
    }

    private PackageEntity parseModule(Element module) throws IOException {
        // there are strange non-breaking spaces before module name sometimes
        String curr = module.child(0).text().replaceAll("^\\p{javaSpaceChar}", "");

        List<PackageEntity> subpackages = parseSubpackages(module);

        if (isIgnorable(curr)) {
            return null;
        }

        Elements moduleLinkElem = module.child(0).getElementsByAttribute("href");
        if (!moduleLinkElem.isEmpty()) {
            String link = moduleLinkElem.get(0).attributes().get("href");
            packageName = curr;
            parse(new URL(BASE_ADDRESS + link));
        }

        PackageEntity pack = new PackageEntity(curr, "Haskell", classesByPackageName(curr),
                functionsByPackageName(curr), subpackages, null,
                (packageDoc.containsKey(curr) ? packageDoc.get(curr) : ""), "");
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
        shortDefinitions.put(packageName, document.getElementById("section.syn"));
        if (shortDefinitions.get(packageName) == null) {
            return;
        }

        packageDoc.put(packageName, getDoc(document.getElementById("description")));

        for (Element elem : document.getElementsByClass("top")) {
            parseElem(elem);
        }
    }

    private void parseElem(Element elem) {
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
                && elem.getElementsMatchingOwnText("^module$").isEmpty()
                && elem.getElementsMatchingOwnText("^type family$").isEmpty();
    }

    private static boolean isTypeClass(Element elem) {
        return !elem.getElementsMatchingOwnText("^class$").isEmpty();
    }

    private static boolean isDataType(Element elem) {
        return !elem.getElementsMatchingOwnText("^data$").isEmpty()
                || !elem.getElementsMatchingOwnText("^newtype$").isEmpty();
    }

    private static boolean isTypeAlias(Element elem) {
        return !elem.getElementsMatchingOwnText("^type$").isEmpty()
                && elem.getElementsMatchingOwnText("^type family$").isEmpty();
    }

    private void parseTypeClass(Element elem) {
        String name = elem.child(0).getElementsByClass("def").get(0).text();
        QualifiedName qualifiedName = new QualifiedName(packageName, name);

        parents.put(qualifiedName, parseTypeClassParents(elem.children().get(0), name));
        parseInstances(elem, name);

        classes.put(qualifiedName, new Classifier(name, "Haskell", getDoc(elem), "", parseFunctions(elem)));
    }

    private List<QualifiedName> parseTypeClassParents(Element element, String parentName) {
        List<QualifiedName> result = new ArrayList<>();

        for (Element parentLink : element.getElementsByAttribute("href")) {
            if (parentLink.hasClass("link")) {
                continue;
            }

            String parentPackage = getPackageName(parentLink.attributes().get("href"));
            QualifiedName parent = new QualifiedName(parentPackage, parentLink.text());
            result.add(parent);
             if (!derived.containsKey(parent)) {
                derived.put(parent, new ArrayList<QualifiedName>());
            }
            derived.get(parent).add(new QualifiedName(packageName, parentName));
        }

        return result;
    }

    String getPackageName(String link) {
        // link has format "http://something/something/Package-Name.html", getting "Package.Name" out of it
        String[] parts = link.split("\\.html")[0].split("/");
        return parts[parts.length - 1].replaceAll("-", ".");
    }

    private void parseInstances(Element elem, String name) {
        if (elem.getElementsByClass("instances").isEmpty()) {
            return;
        }

        for (Element el : elem.getElementsByClass("instances").get(0).getElementsByClass("src")) {
            QualifiedName type = getType(el, name);
            if (type != null) {
                fillInstance(name, type);
            }
        }
    }

    private QualifiedName getType(Element el, String name) {
        // we don't care about typeclass constraints here so we throw them away
        String[] description = el.text().split("\\s+=>\\s+");
        String fullType = description[description.length - 1];

        // TODO: need to handle strange instances somehow
        if (!name.equals(fullType.split("\\s+")[0])) {
            return null;
        }
        // get instance type only
        fullType = fullType.replaceAll("^" + name + "\\s+", "");

        String typeName = getTypeName(fullType);
        if (!Character.isUpperCase(typeName.charAt(0))) {
            return null;
        }

        String packName = OTHER_PACKAGE;
        if (!typeName.equals("List") && !typeName.matches("^Tuple\\d+$")) {
            packName = getPackageName(el.getElementsMatchingOwnText("^" + typeName + "$").get(0).attributes().get("href"));
        }

        return new QualifiedName(packName, typeName);
    }

    private String getTypeName(String fullType) {
        String type = "";
        if (fullType.startsWith("[") && fullType.endsWith("]")) {
            type = "List";
        } else if (fullType.startsWith("(") && fullType.endsWith(")")) {
            fullType = fullType.substring(1, fullType.length() - 1);
            if (fullType.isEmpty()) {
                type = "Tuple0";
            } else if (fullType.equals(new String(new char[fullType.length()]).replace('\0', ','))) {
                type = "Tuple" + String.valueOf(fullType.length() + 1);
            }
        }

        if (type.equals("")) {
            type = fullType.split("\\s+")[0];
        }

        return type;
    }

    private void fillInstance(String name, QualifiedName type) {
        QualifiedName qualifiedName = new QualifiedName(packageName, name);

        if (!derived.containsKey(qualifiedName)) {
            derived.put(qualifiedName, new ArrayList<QualifiedName>());
        }
        if (!derived.get(qualifiedName).contains(type)) {
            derived.get(qualifiedName).add(type);
        }

        if (!parents.containsKey(type)) {
            parents.put(type, new ArrayList<QualifiedName>());
        }
        if (!parents.get(type).contains(qualifiedName)) {
            parents.get(type).add(qualifiedName);
        }
    }

    private List<FunctionEntity> parseFunctions(Element elem) {
        List<FunctionEntity> functions = new ArrayList<>();

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

    private List<FunctionEntity> parseFunctionDef(Element func) {
        // names of functions in the definition (which can contain more than one function, divided by ',')
        String[] names = func.child(0).text().split("::\\s+")[0].split(",\\s+");

        List<FunctionEntity> result = new ArrayList<>();
        for (String name : names) {
            result.add(parseFunction(func, new QualifiedName(packageName, name)));
        }

        return result;
    }

    private FunctionEntity parseFunction(Element func, QualifiedName name) {
        String signature = getSignature(name.getValue());
        fillFunctionType(name, signature);

        Element doc = func.nextElementSibling();
        String d = "";
        if (doc != null) {
            d = doc.text();
        }

        FunctionEntity function = new FunctionEntity(name.getValue(), "Haskell", d, "");
        functions.put(name, function);
        return function;
    }

    private void fillFunctionType(QualifiedName name, String signature) {
        Map<String, ParameterDescription> parameters = parseParameters(signature);
        parseSignature(signature, name, parameters);
        fillInterfaces(shortDefinitions.get(packageName).getElementsMatchingText(
                "^" + Pattern.quote(name.getValue()) + "(\\s|,)").last(), parameters);

        entityParameters.put(name, parameters);
        entityEndParameters.put(name, new HashMap<String, TypeVariable>());
    }

    private Map<String, ParameterDescription> parseParameters(String signature) {
        Map<String, ParameterDescription> parameters = new HashMap<>();
        for (String s : signature.split("\\s+")) {
            s = s.replaceAll("[\\(\\)\\[\\],\\->]", "");  // keep parameters without list-tuple-function artifacts
            if (!s.isEmpty() && Character.isLowerCase(s.charAt(0)) && !parameters.containsKey(s)) {
                parameters.put(s, new ParameterDescription(parameters.size(), new ArrayList<QualifiedName>()));
            }
        }

        return parameters;
    }

    private void parseSignature(String signature, QualifiedName name, Map<String, ParameterDescription> parameters) {
        signature = "(" + signature + ")";
        signatures.put(name, HaskellType.parse(signature, parameters));
    }

    private void fillInterfaces(Element func, Map<String, ParameterDescription> parameters) {
        for (ParameterDescription param : parameters.values()) {
            for (QualifiedName type : param.getValue()) {
                Elements elems = func.getElementsMatchingOwnText("^" + type.getValue() + "$");
                if (!elems.isEmpty()) {
                    param.getValue().set(param.getValue().indexOf(type), new QualifiedName(
                            getPackageName(elems.get(0).attributes().get("href"))
                            , type.getValue()));
                }
            }
        }
    }

    private String getSignature(String name) {
        Element func = shortDefinitions.get(packageName).getElementsMatchingText(
                "^" + Pattern.quote(name) + "$").get(0).parent();
        String[] parts = func.text().split("::\\s*");  // split into name and type description

        for (Element elem : func.getElementsByClass("fixity")) {
            parts[1] = parts[1].replaceAll(elem.text(), "");
        }

        for (Element elem : func.getElementsByClass("rightedge")) {
            parts[1] = parts[1].replaceAll(elem.text(), "");
        }

        for (Element elem : func.getElementsByClass("link")) {
            parts[1] = parts[1].replaceAll(elem.text(), "");
        }

        // ignoring forall specifications for simplicity of further parsing
        return parts[1].replaceAll("forall(\\s+[a-z]+)*\\.\\s+", "").trim();
    }

    private void parseData(Element elem) {
        String name = elem.child(0).getElementsByClass("def").get(0).text();

        // removing links to source, keywords and kind specification
        String def = elem.children().get(0).text().split("\\s+Source")[0].split("\\s+where")[0]
                .split("\\s+::\\s+")[0].replaceFirst("\\s*data\\s+", "").replaceFirst("\\s*newtype\\s+", "");

        parseTypeParameters(elem, def, name);
        parseInterfacesInstances(elem, name);

        classes.put(new QualifiedName(packageName, name), new Classifier(name, "Haskell", getDoc(elem)
                , "", new ArrayList<FunctionEntity>()));
    }

    private void parseTypeParameters(Element elem, String def, String name) {
        String[] parts = def.split("\\s+=>\\s+");
        String[] params = parts[parts.length - 1].split("\\s+");
        if (!params[0].equals(name)) {
            return;
        }

        Map<String, ParameterDescription> parameters = new HashMap<>();
        for (int i = 1; i < params.length; ++i) {
            parameters.put(params[i], new ParameterDescription(i - 1, new ArrayList<QualifiedName>()));
        }

        if (parts.length > 1) {
            String interfacesString = parts[0];
            if (interfacesString.startsWith("(") && interfacesString.endsWith(")")) {
                interfacesString = interfacesString.substring(1, interfacesString.length() - 1);
            }
            String[] interfaces = interfacesString.split(",\\s+");
            for (String anInterface : interfaces) {
                String[] d = anInterface.split("\\s+");
                parameters.get(d[1]).getValue().add(new QualifiedName(
                        getPackageName(elem.getElementsMatchingOwnText("^" + d[0] + "$").get(0).attributes().get("href"))
                        , d[0]));
            }
        }

        entityParameters.put(new QualifiedName(packageName, name), parameters);
    }

    private void parseInterfacesInstances(Element elem, String name) {
        if (elem.getElementsByClass("instances").isEmpty()) {
            return;
        }

        for (Element el : elem.getElementsByClass("instances").get(0).getElementsByClass("src")) {
            String[] description = el.text().split("\\s+=>\\s+");
            String interf = description[description.length - 1].split("\\s+")[0];
            Pair<String, String> type = getType(el, interf);
            if (type != null && type.getValue().equals(name)) {
                fillInstance(interf, new QualifiedName(packageName, name));
            }
        }
    }

    private void parseTypeAlias(Element elem) {
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

        Map<String, ParameterDescription> parameters = parseParameters(parts[1]);
        parseSignature(parts[1], qualifiedName, parameters);

        entityParameters.put(qualifiedName, parameters);
        entityEndParameters.put(qualifiedName, new HashMap<String, TypeVariable>());
        aliases.put(qualifiedName, new TypeAlias(name, "Haskell", getDoc(elem), ""));
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

        for (Map.Entry<QualifiedName, FunctionEntity> func : functions.entrySet()) {
            func.getValue().setContainingPackage(packages.get(func.getKey().getKey()));
            createFunctionConnections(func.getValue());
        }

        for (Map.Entry<QualifiedName, TypeAlias> alias : aliases.entrySet()) {
            alias.getValue().setContainingPackage(packages.get(alias.getKey().getKey()));
            createAliasConnections(alias.getValue());
        }
    }

    private void createClassifierConnections(Classifier classifier) {
        String packName = classifier.getContainingPackage().getName();

        connectDerived(classifier, packName, true);
        connectDerived(classifier, packName, false);
        connectTypeParameters(classifier, packName);

        for (FunctionEntity function : classifier.getFunctions()) {
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
                        classifier.addBase(classes.get(connection));
                    }
                }
            }
        }
    }

    private void connectTypeParameters(Classifier classEntity, String packName) {
        Map<String, ParameterDescription> params = entityParameters.get(new QualifiedName(packName, classEntity.getName()));
        if (params == null) {
            return;
        }

        for (Pair<Integer, List<QualifiedName>> param : params.values()) {
            classEntity.addParameter(new TypeVariable(param.getKey(), paramInterfaceConstraints(param)));
        }
    }

    private List<Classifier> paramInterfaceConstraints(Pair<Integer, List<QualifiedName>> param) {
        List<Classifier> interfaceConstraints = new ArrayList<>();
        for (QualifiedName interf : param.getValue()) {
            if (classes.containsKey(interf)) {
                interfaceConstraints.add(classes.get(interf));
            }
        }
        return interfaceConstraints;
    }

    // TODO: next method, copypaste
    private void createFunctionConnections(FunctionEntity function) {
        QualifiedName funcName = new QualifiedName(function.getContainingPackage().getName(), function.getName());

        Map<String, ParameterDescription> params = entityParameters.get(funcName);
        for (Map.Entry<String, ParameterDescription> param : params.entrySet()) {
            TypeVariable variable = new TypeVariable(param.getValue().getKey(), paramInterfaceConstraints(param.getValue()));
            function.addParameter(variable);
            if (!entityEndParameters.get(funcName).containsKey(param.getKey())) {
                entityEndParameters.get(funcName).put(param.getKey(), variable);
            }
        }

        function.setSignature(signatures.get(funcName).makeSignature(this, funcName, false));
    }

    private void createAliasConnections(TypeAlias alias) {
        QualifiedName name = new QualifiedName(alias.getContainingPackage().getName(), alias.getName());

        Map<String, ParameterDescription> params = entityParameters.get(name);
        for (Map.Entry<String, ParameterDescription> param : params.entrySet()) {
            TypeVariable variable = new TypeVariable(param.getValue().getKey(), paramInterfaceConstraints(param.getValue()));
            alias.addParameter(variable);
            if (!entityEndParameters.get(name).containsKey(param.getKey())) {
                entityEndParameters.get(name).put(param.getKey(), variable);
            }
        }

        alias.setAliasedType(signatures.get(name).buildType(this, name, true));
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

    private List<FunctionEntity> functionsByPackageName(String packName) {
        List<FunctionEntity> result = new ArrayList<>();
        for (Map.Entry<QualifiedName, FunctionEntity> function : functions.entrySet()) {
            if (function.getKey().getKey().equals(packName)) {
                result.add(function.getValue());
            }
        }
        return result;
    }

    private void fillListAndTuples() {
        packageDoc.put(OTHER_PACKAGE, "");
        List<TypeConstructor> otherClasses = new ArrayList<>();
        Classifier list = new Classifier("List", "Haskell", "", "", new ArrayList<FunctionEntity>());
        list.addParameter(new TypeVariable(0, new ArrayList<Classifier>()));
        otherClasses.add(list);
        QualifiedName listQualified = new QualifiedName(OTHER_PACKAGE, "List");
        classes.put(listQualified, list);
        parents.put(listQualified, new ArrayList<QualifiedName>());
        entityParameters.put(listQualified, new HashMap<String, ParameterDescription>());

        for (int i = 0; i <= 63; ++i) {
            if (i == 1) {
                continue;
            }
            String name = "Tuple" + String.valueOf(i);
            Classifier tuple = new Classifier(name, "Haskell", "", "", new ArrayList<FunctionEntity>());
            for (int j = 0; j < i; ++j) {
                tuple.addParameter(new TypeVariable(j, new ArrayList<Classifier>()));
            }
            otherClasses.add(tuple);
            QualifiedName qualifiedName = new QualifiedName(OTHER_PACKAGE, name);
            classes.put(qualifiedName, tuple);
            parents.put(qualifiedName, new ArrayList<QualifiedName>());
            entityParameters.put(qualifiedName, new HashMap<String, ParameterDescription>());
        }

        PackageEntity other = new PackageEntity(OTHER_PACKAGE, "Haskell", otherClasses
                , new ArrayList<FunctionEntity>(), new ArrayList<PackageEntity>(), null, "", "");
        for (TypeConstructor otherClass : otherClasses) {
            // TODO: ugly cast
            ((Classifier) otherClass).setContainingPackage(other);
        }
        packages.put(OTHER_PACKAGE, other);
    }

    private boolean isIgnorable(String moduleName) {
        return moduleName.equals("Data.Type.Coercion") || moduleName.equals("Data.Type.Equality")
                || moduleName.equals("Data.Typeable") || moduleName.equals("GHC.Generics")
                || moduleName.equals("GHC.Integer") || moduleName.equals("GHC.Prim")
                || moduleName.equals("GHC.TypeLits") || moduleName.equals("Data.Typeable.Internal")
                || moduleName.equals("GHC.Integer.GMP.Internals") || moduleName.equals("GHC.Integer.Logarithms");
    }
}