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
    String packageName = "";
    Map<String, Element> shortDefinitions = new HashMap<>();
    Map<String, PackageEntity> packages = new HashMap<>();
    Map<String, String> packageDoc = new HashMap<>();
    Map<QualifiedName, FunctionEntity> functions = new HashMap<>();
    Map<QualifiedName, Classifier> classes = new HashMap<>();
    Map<QualifiedName, List<QualifiedName>> parents = new HashMap<>();
    Map<QualifiedName, List<QualifiedName>> derived = new HashMap<>();
    Map<QualifiedName, Map<String, ParameterDescription>> typeParameters = new HashMap<>();
    Map<QualifiedName, Map<String, ParameterDescription>> functionParameters = new HashMap<>();
    Map<QualifiedName, Map<String, TypeVariable>> functionEndParameters = new HashMap<>();
    Map<QualifiedName, HaskellType> signatures = new HashMap<>();

    public static void main(String[] args) throws IOException {
        HaskellParser parser = new HaskellParser();
        parser.fillListAndTuples();

        Document moduleList = Jsoup.parse(new URL(BASE_ADDRESS), 2000);
        Elements modules = moduleList.getElementById("module-list").getElementsByTag("ul").get(0).children();
        for (Element module : modules) {
            parser.parseModule(module);
        }

        parser.createEntitiesConnections();
    }

    private PackageEntity parseModule(Element module) throws IOException {
        String curr = module.child(0).text().replaceAll("^\\p{javaSpaceChar}", "");

        List<PackageEntity> subpackages = parseSubpackages(module);

        if (isIgnorable(curr)) {
            return null;
        }

        if (!module.child(0).getElementsByAttribute("href").isEmpty()) {
            String link = module.child(0).getElementsByAttribute("href").get(0).attributes().get("href");
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
        Document document = Jsoup.parse(url, 2000);
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
        } else if (isType(elem)) {
            parseType(elem);
        } else if (isFunction(elem)) {
            parseFunctionDef(elem.child(0));
        }
    }

    private boolean isFunction(Element elem) {
        return !isTypeClass(elem) && !isDataType(elem) && !isType(elem)
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

    private static boolean isType(Element elem) {
        return !elem.getElementsMatchingOwnText("^type$").isEmpty();
    }

    private void parseTypeClass(Element elem) {
        String name = elem.child(0).getElementsByClass("def").get(0).text();

        parents.put(new QualifiedName(packageName, name), parseTypeClassParents(elem.children().get(0), name));
        parseInstances(elem, name);

        classes.put(new QualifiedName(packageName, name),
                new Classifier(name, "Haskell", getDoc(elem), parseFunctions(elem), ""));
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
        String[] description = el.text().split("\\s+=>\\s+");

        String fullType = description[description.length - 1];
        if (!name.equals(fullType.split("\\s+")[0])) {
            return null;
        }
        fullType = fullType.replaceAll("^" + name + "\\s+", "");

        String typeName = getTypeName(fullType);
        if (!Character.isUpperCase(typeName.charAt(0))) {
            return null;
        }

        String packName = "other";
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

        if (!elem.getElementsByClass("methods").isEmpty()) {
            for (Element func : elem.getElementsByClass("methods").get(0).getElementsByClass("src")) {
                if (!func.getElementsByClass("def").isEmpty()) {
                    functions.addAll(parseFunctionDef(func));
                }
            }
        }

        return functions;
    }

    private List<FunctionEntity> parseFunctionDef(Element func) {
        String[] names = func.child(0).text().split("::\\s+")[0].split(",\\s+");

        List<FunctionEntity> result = new ArrayList<>();
        for (String name : names) {
            result.add(parseFunction(func, name));
        }

        return result;
    }

    private FunctionEntity parseFunction(Element func, String name) {
        String signature = getSignature(name);
        fillFunctionType(name, signature);

        Element doc = func.nextElementSibling();
        String d = "";
        if (doc != null) {
            d = doc.text();
        }

        FunctionEntity function = new FunctionEntity(name, "Haskell", d, "");
        functions.put(new QualifiedName(packageName, name), function);
        return function;
    }

    private void fillFunctionType(String name, String signature) {
        Map<String, ParameterDescription> parameters = parseParameters(signature);
        parseSignature(signature, name, parameters);
        fillInterfaces(shortDefinitions.get(packageName).getElementsMatchingText(
                "^" + Pattern.quote(name) + "(\\s|,)").last(), parameters);

        QualifiedName qualifiedName = new QualifiedName(packageName, name);
        functionParameters.put(qualifiedName, parameters);
        functionEndParameters.put(qualifiedName, new HashMap<String, TypeVariable>());
    }

    private Map<String, ParameterDescription> parseParameters(String signature) {
        Map<String, ParameterDescription> parameters = new HashMap<>();
        for (String s : signature.split("\\s+")) {
            s = s.replaceAll("[\\(\\)\\[\\],\\->]", "");
            if (!s.isEmpty() && Character.isLowerCase(s.charAt(0)) && !parameters.containsKey(s)) {
                parameters.put(s, new ParameterDescription(parameters.size(), new ArrayList<QualifiedName>()));
            }
        }

        return parameters;
    }

    private void parseSignature(String signature, String funcName, Map<String, ParameterDescription> parameters) {
        signature = "(" + signature + ")";
        signatures.put(new QualifiedName(packageName, funcName), HaskellType.parse(signature, parameters));
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
        String[] parts = func.text().split("::\\s*");

        if (!func.getElementsByClass("fixity").isEmpty()) {
            parts[1] = parts[1].replaceAll(func.getElementsByClass("fixity").get(0).text(), "");
        }

        if (!func.getElementsByClass("rightedge").isEmpty()) {
            parts[1] = parts[1].replaceAll(func.getElementsByClass("rightedge").get(0).text(), "");
        }

        if (!func.getElementsByClass("link").isEmpty()) {
            parts[1] = parts[1].replaceAll(func.getElementsByClass("link").get(0).text(), "");
        }

        return parts[1].replaceAll("forall(\\s+[a-z]+)*\\.\\s+", "").trim();
    }

    private void parseData(Element elem) {
        String name = elem.child(0).getElementsByClass("def").get(0).text();

        String def = elem.children().get(0).text().split("\\s+Source")[0].split("\\s+where")[0]
                .split("\\s+::\\s+")[0].replaceFirst("\\s*data\\s+", "").replaceFirst("\\s*newtype\\s+", "");

        parseTypeParameters(elem, def, name);
        parseInterfacesInstances(elem, name);

        classes.put(new QualifiedName(packageName, name), new Classifier(name, "Haskell", getDoc(elem)
                , new ArrayList<FunctionEntity>(), ""));
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

        typeParameters.put(new QualifiedName(packageName, name), parameters);
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

    private void parseType(Element elem) {
        String name = elem.child(0).getElementsByClass("def").get(0).text();

        classes.put(new QualifiedName(packageName, name), new Classifier(name, "Haskell", getDoc(elem)
                , new ArrayList<FunctionEntity>() /*functions*/, ""));
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
        if (!typeParameters.containsKey(packName)) {
            return;
        }

        Map<String, ParameterDescription> params = typeParameters.get(new QualifiedName(packName, classEntity.getName()));
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

    private void createFunctionConnections(FunctionEntity function) {
        QualifiedName funcName = new QualifiedName(function.getContainingPackage().getName(), function.getName());

        Map<String, ParameterDescription> params = functionParameters.get(funcName);
        for (Map.Entry<String, ParameterDescription> param : params.entrySet()) {
            TypeVariable variable = new TypeVariable(param.getValue().getKey(), paramInterfaceConstraints(param.getValue()));
            function.addParameter(variable);
            if (!functionEndParameters.get(funcName).containsKey(param.getKey())) {
                functionEndParameters.get(funcName).put(param.getKey(), variable);
            }
        }

        function.setSignature(signatures.get(funcName).makeSignature(this, function));
    }

    private List<Classifier> classesByPackageName(String packName) {
        List<Classifier> result = new ArrayList<>();
        for (Map.Entry<QualifiedName, Classifier> function : classes.entrySet()) {
            if (function.getKey().getKey().equals(packName)) {
                result.add(function.getValue());
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
        packageDoc.put("other", "");
        List<Classifier> otherClasses = new ArrayList<>();
        Classifier list = new Classifier("List", "Haskell", "", new ArrayList<FunctionEntity>(), "");
        list.addParameter(new TypeVariable(0, new ArrayList<Classifier>()));
        otherClasses.add(list);
        QualifiedName listQualified = new QualifiedName("other", "List");
        classes.put(listQualified, list);
        parents.put(listQualified, new ArrayList<QualifiedName>());
        typeParameters.put(listQualified, new HashMap<String, ParameterDescription>());

        for (int i = 0; i <= 63; ++i) {
            if (i == 1) {
                continue;
            }
            String name = "Tuple" + String.valueOf(i);
            Classifier tuple = new Classifier(name, "Haskell", "", new ArrayList<FunctionEntity>(), "");
            for (int j = 0; j < i; ++j) {
                tuple.addParameter(new TypeVariable(j, new ArrayList<Classifier>()));
            }
            otherClasses.add(tuple);
            QualifiedName qualifiedName = new QualifiedName("other", name);
            classes.put(qualifiedName, tuple);
            parents.put(qualifiedName, new ArrayList<QualifiedName>());
            typeParameters.put(qualifiedName, new HashMap<String, ParameterDescription>());
        }

        PackageEntity other = new PackageEntity("other", "Haskell", otherClasses
                , new ArrayList<FunctionEntity>(), new ArrayList<PackageEntity>(), null, "", "");
        for (Classifier otherClass : otherClasses) {
            otherClass.setContainingPackage(other);
        }
        packages.put("other", other);
    }

    private boolean isIgnorable(String moduleName) {
        return moduleName.equals("Data.Type.Coercion") || moduleName.equals("Data.Type.Equality")
                || moduleName.equals("Data.Typeable") || moduleName.equals("GHC.Generics")
                || moduleName.equals("GHC.Integer") || moduleName.equals("GHC.Prim")
                || moduleName.equals("GHC.TypeLits") || moduleName.equals("Data.Typeable.Internal")
                || moduleName.equals("GHC.Integer.GMP.Internals") || moduleName.equals("GHC.Integer.Logarithms");
    }
}