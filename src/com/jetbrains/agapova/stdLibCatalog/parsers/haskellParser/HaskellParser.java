package com.jetbrains.agapova.stdLibCatalog.parsers.haskellParser;

import com.jetbrains.agapova.stdLibCatalog.domain.*;
import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Created by ashatta on 7/14/14.
 */
public class HaskellParser {
    Map<String, InterfaceEntity> interfaces = new HashMap<>();
    Map<String, FunctionEntity> functions = new HashMap<>();
    Map<String, ClassEntity> classes = new HashMap<>();
    Map<String, List<String>> parents = new HashMap<>();
    Map<String, List<String>> derived = new HashMap<>();
    Map<String, List<String>> instances = new HashMap<>();
    Map<String, List<String>> typeClasses = new HashMap<>();
    Map<String, Map<String, Pair<Integer, List<String>>>> functionParameters = new HashMap<>();
    Map<String, Map<String, Pair<Integer, List<String>>>> typeParameters = new HashMap<>();
    Map<String, HaskellType> signatures = new HashMap<>();

    public static void main(String[] args) {
        HaskellParser parser = new HaskellParser();
        parser.fillListAndTuples();

        try {
            parser.parse(new URL("http://www.haskell.org/ghc/docs/latest/html/libraries/base-4.7.0.1/Data-Fixed.html"));
        } catch (java.io.IOException m) {
            return;
        }

        parser.createEntitiesConnections();
    }

    public void parse(URL url) throws IOException {
        Document document = Jsoup.parse(url, 2000);

        Elements topElems = document.getElementsByClass("top");

        for (Element elem : topElems) {
            if (isTypeClass(elem)) {
                parseTypeClass(elem);
            } else if (isDataType(elem)) {
                parseData(elem);
            } else if (isType(elem)) {
                parseType(elem);
            } else {
                parseFunction(elem.child(0));
            }
        }
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
        parents.put(name, parseTypeClassParents(elem.children().get(0), name));

        parseInstances(elem, name);

        interfaces.put(name, new InterfaceEntity("", name, "Haskell", doc, parseFunctions(elem), new ArrayList<TypeEntity>() /*derived*/
                , new ArrayList<TypeEntity>() /*parents*/, null, new ArrayList<TypedEntity>() /*parameters*/
                , "", new ArrayList<ClassEntity>() /*instances*/));
    }

    private List<String> parseTypeClassParents(Element element, String parentName) {
        List<String> result = new ArrayList<>();

        for (Element child : element.getElementsByAttribute("href")) {
            if (!child.hasClass("link")) {
                result.add(child.text());
                if (!derived.containsKey(child.text())) {
                    derived.put(child.text(), new ArrayList<String>());
                }
                derived.get(child.text()).add(parentName);
            }
        }

        return result;
    }

    private void parseInstances(Element elem, String name) {
        for (Element el : elem.getElementsByClass("instances").get(0).getElementsByClass("src")) {
            String type = getType(el, name);
            if (type == null) {
                continue;
            }

            fillInstance(name, type);
        }
    }

    private String getType(Element el, String name) {
        String[] description = el.text().split("\\s+=>\\s+");

        String fullType = description[description.length - 1];
        if (!name.equals(fullType.split("\\s+")[0])) {
            return null;
        }
        fullType = fullType.replaceAll("^" + name + "\\s+", "");

        return getTypeName(fullType);
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

    private void fillInstance(String name, String type) {
        if (!instances.containsKey(name)) {
            instances.put(name, new ArrayList<String>());
        }
        if (!instances.get(name).contains(type)) {
            instances.get(name).add(type);
        }

        if (!typeClasses.containsKey(type)) {
            typeClasses.put(type, new ArrayList<String>());
        }
        if (!typeClasses.get(type).contains(name)) {
            typeClasses.get(type).add(name);
        }
    }

    private List<FunctionEntity> parseFunctions(Element elem) {
        List<FunctionEntity> functions = new ArrayList<>();

        Elements funcElems = elem.getElementsByClass("methods").get(0).getElementsByClass("src");
        for (Element func : funcElems) {
            if (func.hasClass("src")) {
                functions.add(parseFunction(func));
            }
        }

        return functions;
    }

    private FunctionEntity parseFunction(Element func) {
        String name = func.getElementsByClass("def").get(0).text();
        String[] description = getFunctionDescription(func);
        String signature = description[description.length - 1];

        Map<String, Pair<Integer, List<String>>> parameters = parseParameters(signature);
        if (description.length > 1) {
            parseInterfaces(description[0], parameters);
        }
        functionParameters.put(name, parameters);

        parseSignature(signature, name);

        Element doc = func.nextElementSibling();
        String d = "";
        if (doc != null) {
            d = doc.text();
        }

        FunctionEntity function = new FunctionEntity("", name, "Haskell", null /*signature*/, d, null /*parent*/, null /*package*/, "", new ArrayList<TypedEntity>());
        functions.put(name, function);
        return function;
    }

    private Map<String, Pair<Integer, List<String>>> parseParameters(String signature) {
        Map<String, Pair<Integer, List<String>>> parameters = new HashMap<>();
        int num = 0;
        for (String s : signature.split("\\s+")) {
            s = s.replaceAll("[(),->]", "");
            if (!s.isEmpty() && Character.isLowerCase(s.charAt(0)) && !parameters.containsKey(s)) {
                parameters.put(s, new Pair<Integer, List<String>>(num++, new ArrayList<String>()));
            }
        }

        return parameters;
    }

    private void parseInterfaces(String description, Map<String, Pair<Integer, List<String>>> parameters) {
        String[] classes = description.replaceAll("[()]", "").split(",\\s+");
        for (String cl : classes) {
            String[] def = cl.split("\\s+");
            parameters.get(def[1]).getValue().add(def[0]);
        }
    }

    private void parseSignature(String signature, String funcName) {
        signature = "(" + signature + ")";
        HaskellType type = HaskellType.parse(signature);
        if (type == null) {
            throw new NullPointerException();
        }
        signatures.put(funcName, type);
    }

    private String[] getFunctionDescription(Element func) {
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
        parts = parts[1].split("\\.\\s+");

        return parts[parts.length - 1].trim().split("\\s+=>\\s+");
    }

    private void parseData(Element elem) {
        String doc = getDoc(elem);
        String name = elem.child(0).getElementsByClass("def").get(0).text();

        String def = elem.children().get(0).text().split("\\s+Source")[0].split("\\s+where")[0]
                .split("\\s+::\\s+")[0].replaceFirst("\\s*data\\s+", "").replaceFirst("\\s*newtype\\s+", "");

        parseTypeParameters(def, name);
        parseInterfacesInstances(elem, name);

        classes.put(name, new ClassEntity("", name, "Haskell", doc, new ArrayList<FunctionEntity>() /*functions*/
                , new ArrayList<TypeEntity>() /*derived*/, new ArrayList<TypeEntity>() /*base*/, null
                , new ArrayList<TypedEntity>() /*parameter*/, "", new ArrayList<InterfaceEntity>() /*interfaces*/));
    }

    private void parseInterfacesInstances(Element elem, String name) {
        for (Element el : elem.getElementsByClass("instances").get(0).getElementsByClass("src")) {
            String[] description = el.text().split("\\s+=>\\s+");
            String interf = description[description.length - 1].split("\\s+")[0];
            if (getType(el, interf).equals(name)) {
                fillInstance(interf, name);
            }
        }
    }

    private void parseTypeParameters(String def, String name) {
        String[] parts = def.split("\\s+=>\\s+");
        String[] params = parts[parts.length - 1].split("\\s+");
        if (!params[0].equals(name)) {
            return;
        }

        Map<String, Pair<Integer, List<String>>> parameters = new HashMap<>();
        for (int i = 1; i < params.length; ++i) {
            parameters.put(params[i], new Pair<Integer, List<String>>(i - 1, new ArrayList<String>()));
        }

        if (parts.length > 1) {
            String interfacesString = parts[0];
            if (interfacesString.startsWith("(") && interfacesString.endsWith(")")) {
                interfacesString = interfacesString.substring(1, interfacesString.length() - 1);
            }
            String[] interfaces = interfacesString.split(",\\s+");
            for (int i = 0; i < interfaces.length; ++i) {
                String[] d = interfaces[i].split("\\s+");
                parameters.get(d[1]).getValue().add(d[0]);
            }
        }

        typeParameters.put(name, parameters);
    }

    private void parseType(Element elem) {
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
        for (InterfaceEntity typeClass : interfaces.values()) {
            createInterfacesConnections(typeClass);
        }
        for (FunctionEntity function : functions.values()) {
            createFunctionsConnections(function);
        }
        for (ClassEntity classEntity : classes.values()) {
            createClassConnections(classEntity);
        }
    }

    private void createClassConnections(ClassEntity classEntity) {
        for (String instance : typeClasses.get(classEntity.getName())) {
            if (interfaces.containsKey(instance)) {
                classEntity.addInterface(interfaces.get(instance));
            }
        }

        Map<String, Pair<Integer, List<String>>> params = typeParameters.get(classEntity.getName());
        for (Map.Entry<String, Pair<Integer, List<String>>> param : params.entrySet()) {
            List<InterfaceEntity> interfaceConstraints = new ArrayList<>();
            for (String interf : param.getValue().getValue()) {
                if (interfaces.containsKey(interf)) {
                    interfaceConstraints.add(interfaces.get(interf));
                }
            }
            classEntity.addParameter(new Parameter(param.getValue().getKey(), interfaceConstraints));
        }
    }

    private void createInterfacesConnections(InterfaceEntity typeClass) {
        if (derived.containsKey(typeClass.getName())) {
            for (String derivedClass : derived.get(typeClass.getName())) {
                if (interfaces.containsKey(derivedClass)) {
                    typeClass.addDerived(interfaces.get(derivedClass));
                }
            }
        }

        for (String baseClass : parents.get(typeClass.getName())) {
            if (interfaces.containsKey(baseClass)) {
                typeClass.addBase(interfaces.get(baseClass));
            }
        }

        for (String instance : instances.get(typeClass.getName())) {
            if (classes.containsKey(instance)) {
                typeClass.addSupporting(classes.get(instance));
            }
        }

        for (FunctionEntity function : typeClass.getFunctions()) {
            function.setContainingType(typeClass);
        }
    }

    private void createFunctionsConnections(FunctionEntity function) {
        Map<String, Pair<Integer, List<String>>> params = functionParameters.get(function.getName());
        for (Map.Entry<String, Pair<Integer, List<String>>> param : params.entrySet()) {
            List<InterfaceEntity> interfaceConstraints = new ArrayList<>();
            for (String interf : param.getValue().getValue()) {
                if (interfaces.containsKey(interf)) {
                    interfaceConstraints.add(interfaces.get(interf));
                }
            }
            function.addParameter(new Parameter(param.getValue().getKey(), interfaceConstraints));
        }
    }

    private void fillListAndTuples() {
        List<TypedEntity> listParam = new ArrayList<>();
        listParam.add(new Parameter(0, new ArrayList<InterfaceEntity>()));
        classes.put("List", new ClassEntity("", "List", "Haskell", "", new ArrayList<FunctionEntity>()
                , new ArrayList<TypeEntity>(), new ArrayList<TypeEntity>(), null, listParam, ""
                , new ArrayList<InterfaceEntity>()));
        typeClasses.put("List", new ArrayList<String>());
        typeParameters.put("List", new HashMap<String, Pair<Integer, List<String>>>());

        for (int i = 0; i <= 63; ++i) {
            if (i == 1) {
                continue;
            }
            List<TypedEntity> tupleParams = new ArrayList<>();
            for (int j = 0; j < i; ++j) {
                tupleParams.add(new Parameter(j, new ArrayList<InterfaceEntity>()));
            }
            String name = "Tuple" + String.valueOf(i);
            classes.put(name, new ClassEntity("", name, "Haskell"
                    , "", new ArrayList<FunctionEntity>(), new ArrayList<TypeEntity>(), new ArrayList<TypeEntity>()
                    , null, tupleParams, "", new ArrayList<InterfaceEntity>()));
            typeClasses.put(name, new ArrayList<String>());
            typeParameters.put(name, new HashMap<String, Pair<Integer, List<String>>>());
        }
    }
}