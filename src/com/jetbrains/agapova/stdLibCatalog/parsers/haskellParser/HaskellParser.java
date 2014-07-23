package com.jetbrains.agapova.stdLibCatalog.parsers.haskellParser;

import com.jetbrains.agapova.stdLibCatalog.domain.*;
import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by ashatta on 7/14/14.
 */
public class HaskellParser {
    Map<String, PackageEntity> packages = new HashMap<>();
    Map<String, String> packageDoc = new HashMap<>();
    Map<String, Map<String, InterfaceEntity>> interfaces = new HashMap<>();
    Map<String, Map<String, FunctionEntity>> functions = new HashMap<>();
    Map<String, Map<String, ClassEntity>> classes = new HashMap<>();
    Map<String, Map<String, List<Pair<String, String>>>> parents = new HashMap<>();
    Map<String, Map<String, List<Pair<String, String>>>> derived = new HashMap<>();
    Map<String, Map<String, List<Pair<String, String>>>> instances = new HashMap<>();
    Map<String, Map<String, List<Pair<String, String>>>> typeClasses = new HashMap<>();
    Map<String, Map<String, Map<String, Pair<Integer, List<Pair<String, String>>>>>> functionParameters = new HashMap<>();
    Map<String, Map<String, Map<String, Parameter>>> functionEndParameters = new HashMap<>();
    Map<String, Map<String, Map<String, Pair<Integer, List<Pair<String, String>>>>>> typeParameters = new HashMap<>();
    Map<String, Map<String, HaskellType>> signatures = new HashMap<>();
    static String baseAddress = "http://www.haskell.org/ghc/docs/latest/html/libraries/";
    String packageName = "";
    Map<String, Element> shortDefinitions = new HashMap<>();

    public static void main(String[] args) {
        HaskellParser parser = new HaskellParser();
        parser.fillListAndTuples();

        try {
            Document moduleList = Jsoup.parse(new URL(baseAddress), 2000);
            Elements modules = moduleList.getElementById("module-list").getElementsByTag("ul").get(0).children();
            for (Element module : modules) {
                parser.parseModule(module);
            }
        } catch (java.io.IOException m) {
            return;
        }

        parser.createEntitiesConnections();
    }

    private PackageEntity parseModule(Element module) throws IOException {
        String curr = module.child(0).text().replaceAll("^\\p{javaSpaceChar}", "");
        initPackage(curr);

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

        if (curr.equals("Data.Type.Coercion") || curr.equals("Data.Type.Equality")
                || curr.equals("Data.Typeable") || curr.equals("GHC.Generics")
                || curr.equals("GHC.Integer") || curr.equals("GHC.Prim")
                || curr.equals("GHC.TypeLits") || curr.equals("Data.Typeable.Internal")
                || curr.equals("GHC.Integer.GMP.Internals") || curr.equals("GHC.Integer.Logarithms")) {
            return null;
        }

        if (!module.child(0).getElementsByAttribute("href").isEmpty()) {
            String link = module.child(0).getElementsByAttribute("href").get(0).attributes().get("href");
            packageName = curr;
            parse(new URL(baseAddress + link));
        }

        PackageEntity pack = new PackageEntity("", curr, "Haskell", new ArrayList<>(classes.get(curr).values())
                , new ArrayList<>(interfaces.get(curr).values()), new ArrayList<>(functions.get(curr).values())
                , subpackages, null, packageDoc.get(curr), "");
        packages.put(curr, pack);
        for (PackageEntity subpackage : subpackages) {
            subpackage.setContainingPackage(pack);
        }
        return pack;
    }

    public void parse(URL url) throws IOException {
        Document document = Jsoup.parse(url, 2000);
        shortDefinitions.put(packageName, document.getElementById("section.syn"));
        if (shortDefinitions.get(packageName) == null) {
            return;
        }

        String doc = "";
        if (document.getElementById("description") != null) {
            Elements docs = document.getElementById("description").getElementsByClass("doc");
            if (!docs.isEmpty()) {
                doc = docs.get(0).text();
            }
        }
        packageDoc.put(this.packageName, doc);

        Elements topElems = document.getElementsByClass("top");

        for (Element elem : topElems) {
            if (isTypeClass(elem)) {
                parseTypeClass(elem);
            } else if (isDataType(elem)) {
                parseData(elem);
            } else if (isType(elem)) {
                parseType(elem);
            } else if (isFunction(elem)) {
                parseFunction(elem.child(0));
            }
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
        String doc = getDoc(elem);
        String name = elem.child(0).getElementsByClass("def").get(0).text();
        parents.get(packageName).put(name, parseTypeClassParents(elem.children().get(0), name));

        parseInstances(elem, name);

        interfaces.get(packageName).put(name, new InterfaceEntity("", name, "Haskell", doc, parseFunctions(elem), new ArrayList<TypeEntity>() /*derived*/
                , new ArrayList<TypeEntity>() /*parents*/, null, new ArrayList<TypedEntity>() /*parameters*/
                , "", new ArrayList<ClassEntity>() /*instances*/));
    }

    private List<Pair<String, String>> parseTypeClassParents(Element element, String parentName) {
        List<Pair<String, String>> result = new ArrayList<>();

        for (Element child : element.getElementsByAttribute("href")) {
            if (!child.hasClass("link")) {
                String parentPackage = getPackageName(child.attributes().get("href"));
                result.add(new Pair<>(parentPackage, child.text()));
                if (!derived.containsKey(parentPackage)) {
                    derived.put(parentPackage, new HashMap<String, List<Pair<String, String>>>());
                }
                if (!derived.get(parentPackage).containsKey(child.text())) {
                    derived.get(parentPackage).put(child.text(), new ArrayList<Pair<String, String>>());
                }
                derived.get(parentPackage).get(child.text()).add(new Pair<>(packageName, parentName));
            }
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
            Pair<String, String> type = getType(el, name);
            if (type == null) {
                continue;
            }

            fillInstance(name, type);
        }
    }

    private Pair<String, String> getType(Element el, String name) {
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

        return new Pair<>(packName, typeName);
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

    private void fillInstance(String name, Pair<String, String> type) {
        if (!instances.get(packageName).containsKey(name)) {
            instances.get(packageName).put(name, new ArrayList<Pair<String, String>>());
        }
        if (!instances.get(packageName).get(name).contains(type)) {
            instances.get(packageName).get(name).add(type);
        }

        if (!typeClasses.containsKey(type.getKey())) {
            typeClasses.put(type.getKey(), new HashMap<String, List<Pair<String, String>>>());
        }
        if (!typeClasses.get(type.getKey()).containsKey(type.getValue())) {
            typeClasses.get(type.getKey()).put(type.getValue(), new ArrayList<Pair<String, String>>());
        }
        if (!typeClasses.get(type.getKey()).get(type.getValue()).contains(new Pair<>(packageName, name))) {
            typeClasses.get(type.getKey()).get(type.getValue()).add(new Pair<>(packageName, name));
        }
    }

    private List<FunctionEntity> parseFunctions(Element elem) {
        List<FunctionEntity> functions = new ArrayList<>();

        if (!elem.getElementsByClass("methods").isEmpty()) {
            Elements funcElems = elem.getElementsByClass("methods").get(0).getElementsByClass("src");
            for (Element func : funcElems) {
                if (!func.getElementsByClass("def").isEmpty()) {
                    functions.addAll(parseFunction(func));
                }
            }
        }

        return functions;
    }

    private List<FunctionEntity> parseFunction(Element func) {
        String[] names = func.child(0).text().split("::\\s+")[0].split(",\\s+");
        List<FunctionEntity> result = new ArrayList<>();
        for (String name : names) {
            String signature = getSignature(name);

            Map<String, Pair<Integer, List<Pair<String, String>>>> parameters = parseParameters(signature);
            parseSignature(signature, name, parameters);
            fillInterfaces(shortDefinitions.get(packageName).getElementsMatchingText(
                    "^" + Pattern.quote(name) + "(\\s|,)").last(), parameters);

            functionParameters.get(packageName).put(name, parameters);
            functionEndParameters.get(packageName).put(name, new HashMap<String, Parameter>());

            Element doc = func.nextElementSibling();
            String d = "";
            if (doc != null) {
                d = doc.text();
            }

            FunctionEntity function = new FunctionEntity("", name, "Haskell", null /*signature*/, d, null /*parent*/, null /*package*/, "", new ArrayList<TypedEntity>());
            functions.get(packageName).put(name, function);
            result.add(function);
        }

        return result;
    }

    private Map<String, Pair<Integer, List<Pair<String, String>>>> parseParameters(String signature) {
        Map<String, Pair<Integer, List<Pair<String, String>>>> parameters = new HashMap<>();
        for (String s : signature.split("\\s+")) {
            s = s.replaceAll("[\\(\\)\\[\\],\\->]", "");
            if (!s.isEmpty() && Character.isLowerCase(s.charAt(0)) && !parameters.containsKey(s)) {
                parameters.put(s, new Pair<Integer, List<Pair<String, String>>>(parameters.size()
                        , new ArrayList<Pair<String, String>>()));
            }
        }

        return parameters;
    }

    private void parseSignature(String signature, String funcName, Map<String, Pair<Integer, List<Pair<String, String>>>> parameters) {
        signature = "(" + signature + ")";
        HaskellType type = HaskellType.parse(signature, parameters);
        if (type == null) {
            throw new NullPointerException();
        }
        signatures.get(packageName).put(funcName, type);
    }

    private void fillInterfaces(Element func, Map<String, Pair<Integer, List<Pair<String, String>>>> parameters) {
        for (Pair<Integer, List<Pair<String, String>>> param : parameters.values()) {
            for (Pair<String, String> type : param.getValue()) {
                Elements elems = func.getElementsMatchingText("^" + type.getValue() + "$");
                if (!elems.isEmpty()) {
                    Elements fit = func.getElementsMatchingOwnText("^" + type.getValue() + "$");
                    param.getValue().set(param.getValue().indexOf(type), new Pair<>(
                            getPackageName(func.getElementsMatchingOwnText("^" + type.getValue() + "$").get(0).attributes().get("href"))
                            , type.getValue()));
                }
            }
        }
    }

    private String getSignature(String name) {
        Element func = shortDefinitions.get(packageName).getElementsMatchingText("^" + Pattern.quote(name) + "$").get(0).parent();
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

        String tmp = parts[1].replaceAll("forall(\\s+[a-z]+)*\\.\\s+", "").trim();
        return parts[1].replaceAll("forall(\\s+[a-z]+)*\\.\\s+", "").trim();
    }

    private void parseData(Element elem) {
        String doc = getDoc(elem);
        String name = elem.child(0).getElementsByClass("def").get(0).text();

        String def = elem.children().get(0).text().split("\\s+Source")[0].split("\\s+where")[0]
                .split("\\s+::\\s+")[0].replaceFirst("\\s*data\\s+", "").replaceFirst("\\s*newtype\\s+", "");

        parseTypeParameters(elem, def, name);
        parseInterfacesInstances(elem, name);

        classes.get(packageName).put(name, new ClassEntity("", name, "Haskell", doc, new ArrayList<FunctionEntity>() /*functions*/
                , new ArrayList<TypeEntity>() /*derived*/, new ArrayList<TypeEntity>() /*base*/, null
                , new ArrayList<TypedEntity>() /*parameter*/, "", new ArrayList<InterfaceEntity>() /*interfaces*/));
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
                fillInstance(interf, new Pair<>(packageName, name));
            }
        }
    }

    private void parseTypeParameters(Element elem, String def, String name) {
        String[] parts = def.split("\\s+=>\\s+");
        String[] params = parts[parts.length - 1].split("\\s+");
        if (!params[0].equals(name)) {
            return;
        }

        Map<String, Pair<Integer, List<Pair<String, String>>>> parameters = new HashMap<>();
        for (int i = 1; i < params.length; ++i) {
            parameters.put(params[i], new Pair<Integer, List<Pair<String, String>>>(i - 1
                    , new ArrayList<Pair<String, String>>()));
        }

        if (parts.length > 1) {
            String interfacesString = parts[0];
            if (interfacesString.startsWith("(") && interfacesString.endsWith(")")) {
                interfacesString = interfacesString.substring(1, interfacesString.length() - 1);
            }
            String[] interfaces = interfacesString.split(",\\s+");
            for (String anInterface : interfaces) {
                String[] d = anInterface.split("\\s+");
                parameters.get(d[1]).getValue().add(new Pair<>(
                        getPackageName(elem.getElementsMatchingOwnText("^" + d[0] + "$").get(0).attributes().get("href"))
                        , d[0]));
            }
        }

        typeParameters.get(packageName).put(name, parameters);
    }

    private void parseType(Element elem) {
        String doc = getDoc(elem);
        String name = elem.child(0).getElementsByClass("def").get(0).text();

        classes.get(packageName).put(name, new ClassEntity("", name, "Haskell", doc, new ArrayList<FunctionEntity>() /*functions*/
                , new ArrayList<TypeEntity>() /*derived*/, new ArrayList<TypeEntity>() /*base*/, null
                , new ArrayList<TypedEntity>() /*parameter*/, "", new ArrayList<InterfaceEntity>() /*interfaces*/));
    }

    private static String getDoc(Element elem) {
        Elements docs = elem.getElementsByClass("doc");
        String doc = "";
        if (!docs.isEmpty()) {
            doc = docs.get(0).text();
        }

        return doc;
    }

    private void createEntitiesConnections() {
        for (Map.Entry<String, Map<String, InterfaceEntity>> entity : interfaces.entrySet()) {
            for (InterfaceEntity typeClass : entity.getValue().values()) {
                typeClass.setContainingPackage(packages.get(entity.getKey()));
                createInterfacesConnections(typeClass);
            }
        }

        for (Map.Entry<String, Map<String, FunctionEntity>> entity : functions.entrySet()) {
            for (FunctionEntity function : entity.getValue().values()) {
                function.setContainingPackage(packages.get(entity.getKey()));
                createFunctionsConnections(function);
            }
        }

        for (Map.Entry<String, Map<String, ClassEntity>> entity : classes.entrySet()) {
            for (ClassEntity classEntity : entity.getValue().values()) {
                classEntity.setContainingPackage(packages.get(entity.getKey()));
                createClassConnections(classEntity);
            }
        }
    }

    private void createClassConnections(ClassEntity classEntity) {
        String packName = classEntity.getContainingPackage().getName();
        if (typeClasses.get(packName).containsKey(classEntity.getName())) {
            for (Pair<String, String> instance : typeClasses.get(packName).get(classEntity.getName())) {
                if (interfaces.get(instance.getKey()).containsKey(instance.getValue())) {
                    classEntity.addInterface(interfaces.get(instance.getKey()).get(instance.getValue()));
                }
            }
        }

        Map<String, Pair<Integer, List<Pair<String, String>>>> params = typeParameters.get(packName).get(classEntity.getName());
        if (params != null) {
            for (Map.Entry<String, Pair<Integer, List<Pair<String, String>>>> param : params.entrySet()) {
                List<InterfaceEntity> interfaceConstraints = new ArrayList<>();
                for (Pair<String, String> interf : param.getValue().getValue()) {
                    if (interfaces.get(interf.getKey()).containsKey(interf.getValue())) {
                        interfaceConstraints.add(interfaces.get(interf.getKey()).get(interf.getValue()));
                    }
                }
                classEntity.addParameter(new Parameter(param.getValue().getKey(), interfaceConstraints));
            }
        }
    }

    private void createInterfacesConnections(InterfaceEntity typeClass) {
        String packName = typeClass.getContainingPackage().getName();
        if (derived.get(packName).containsKey(typeClass.getName())) {
            for (Pair<String, String> derivedClass : derived.get(packName).get(typeClass.getName())) {
                if (interfaces.get(derivedClass.getKey()).containsKey(derivedClass.getValue())) {
                    typeClass.addDerived(interfaces.get(derivedClass.getKey()).get(derivedClass.getValue()));
                }
            }
        }

        for (Pair<String, String> baseClass : parents.get(packName).get(typeClass.getName())) {
            if (interfaces.containsKey(baseClass.getKey())
                    && interfaces.get(baseClass.getKey()).containsKey(baseClass.getValue())) {
                typeClass.addBase(interfaces.get(baseClass.getKey()).get(baseClass.getValue()));
            }
        }

        if (instances.get(packName).containsKey(typeClass.getName())) {
            for (Pair<String, String> instance : instances.get(packName).get(typeClass.getName())) {
                if (classes.containsKey(instance.getKey())
                        && classes.get(instance.getKey()).containsKey(instance.getValue())) {
                    typeClass.addSupporting(classes.get(instance.getKey()).get(instance.getValue()));
                }
            }
        }

        for (FunctionEntity function : typeClass.getFunctions()) {
            function.setContainingType(typeClass);
        }
    }

    private void createFunctionsConnections(FunctionEntity function) {
        String packName = function.getContainingPackage().getName();
        Map<String, Pair<Integer, List<Pair<String, String>>>> params = functionParameters.get(packName).get(function.getName());
        for (Map.Entry<String, Pair<Integer, List<Pair<String, String>>>> param : params.entrySet()) {
            List<InterfaceEntity> interfaceConstraints = new ArrayList<>();
            for (Pair<String, String> interf : param.getValue().getValue()) {
                if (interfaces.containsKey(interf.getKey())
                        && interfaces.get(interf.getKey()).containsKey(interf.getValue())) {
                    interfaceConstraints.add(interfaces.get(interf.getKey()).get(interf.getValue()));
                }
            }
            function.addParameter(new Parameter(param.getValue().getKey(), interfaceConstraints));
        }

        function.setSignature(signatures.get(function.getContainingPackage().getName()).get(function.getName())
                .makeSignature(this, function));
    }

    private void fillListAndTuples() {
        initPackage("other");
        List<ClassEntity> otherClasses = new ArrayList<>();
        List<TypedEntity> listParam = new ArrayList<>();
        listParam.add(new Parameter(0, new ArrayList<InterfaceEntity>()));
        ClassEntity list = new ClassEntity("", "List", "Haskell", "", new ArrayList<FunctionEntity>()
                , new ArrayList<TypeEntity>(), new ArrayList<TypeEntity>(), null, listParam, ""
                , new ArrayList<InterfaceEntity>());
        otherClasses.add(list);
        classes.get("other").put("List", list);
        typeClasses.get("other").put("List", new ArrayList<Pair<String, String>>());
        typeParameters.get("other").put("List", new HashMap<String, Pair<Integer, List<Pair<String, String>>>>());

        for (int i = 0; i <= 63; ++i) {
            if (i == 1) {
                continue;
            }
            List<TypedEntity> tupleParams = new ArrayList<>();
            for (int j = 0; j < i; ++j) {
                tupleParams.add(new Parameter(j, new ArrayList<InterfaceEntity>()));
            }
            String name = "Tuple" + String.valueOf(i);
            ClassEntity tuple = new ClassEntity("", name, "Haskell"
                    , "", new ArrayList<FunctionEntity>(), new ArrayList<TypeEntity>(), new ArrayList<TypeEntity>()
                    , null, tupleParams, "", new ArrayList<InterfaceEntity>());
            otherClasses.add(tuple);
            classes.get("other").put(name, tuple);
            typeClasses.get("other").put(name, new ArrayList<Pair<String, String>>());
            typeParameters.get("other").put(name, new HashMap<String, Pair<Integer, List<Pair<String, String>>>>());
        }

        PackageEntity other = new PackageEntity("", "other", "Haskell", otherClasses, new ArrayList<InterfaceEntity>()
                , new ArrayList<FunctionEntity>(), new ArrayList<PackageEntity>(), null, "", "");
        for (ClassEntity otherClass : otherClasses) {
            otherClass.setContainingPackage(other);
        }
        packages.put("other", other);
    }

    private void initPackage(String name) {
        interfaces.put(name, new HashMap<String, InterfaceEntity>());
        functions.put(name, new HashMap<String, FunctionEntity>());
        classes.put(name, new HashMap<String, ClassEntity>());
        parents.put(name, new HashMap<String, List<Pair<String, String>>>());
        if (!derived.containsKey(name)) {
            derived.put(name, new HashMap<String, List<Pair<String, String>>>());
        }
        instances.put(name, new HashMap<String, List<Pair<String, String>>>());
        if (!typeClasses.containsKey(name)) {
            typeClasses.put(name, new HashMap<String, List<Pair<String, String>>>());
        }
        functionEndParameters.put(name, new HashMap<String, Map<String, Parameter>>());
        functionParameters.put(name, new HashMap<String, Map<String, Pair<Integer, List<Pair<String, String>>>>>());
        typeParameters.put(name, new HashMap<String, Map<String, Pair<Integer, List<Pair<String, String>>>>>());
        signatures.put(name, new HashMap<String, HaskellType>());
    }
}