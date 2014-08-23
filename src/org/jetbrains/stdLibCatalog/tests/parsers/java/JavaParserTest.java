package org.jetbrains.stdLibCatalog.tests.parsers.java;

import org.apache.commons.io.FileUtils;
import org.jetbrains.stdLibCatalog.domain.Classifier;
import org.jetbrains.stdLibCatalog.domain.MemberEntity;
import org.jetbrains.stdLibCatalog.domain.PackageEntity;
import org.jetbrains.stdLibCatalog.domain.TypeConstructor;
import org.jetbrains.stdLibCatalog.parsers.java.JavaParser;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.QualifiedName;

public class JavaParserTest {
    private final String TEST_FOLDER = "resources/tests/parsers/java/";
    private static JavaParser parser;

    @BeforeClass
    public static void setUp() throws IOException {
        parser = new JavaParser();
        parser.parse();
    }

    @Test
    public void testMethodWithoutParametersWithPrimitiveResult() throws IOException {
        testClassMember("java.util", "Currency", "getDefaultFractionDigits--");
    }

    @Test
    public void testSimpleMethodWithOneParameter() throws IOException {
        testClassMember("java.util", "Stack", "search-java.lang.Object-");
    }

    @Test
    public void testMethodWithMultipleParametersUsingClassTypeVariables() throws IOException {
        testClassMember("java.util", "TreeMap", "subMap-K-boolean-K-boolean-");
    }

    @Test
    public void testInterfaceMethodWithoutWildcardsDefault() throws IOException {
        testClassMember("java.util", "Map", "computeIfPresent-K-java.util.function.BiFunction-");
    }

    @Test
    public void testMethodWithTypeVariablesAndBoundedWildcard() throws IOException {
        testClassMember("java.util", "Map.Entry", "comparingByKey--");
    }

    @Test
    public void testMethodWithArrayParameters() throws IOException {
        testClassMember("javax.management", "DynamicMBean", "invoke-java.lang.String-java.lang.Object:A-java.lang.String:A-");
    }

    @Test
    public void testConstructorWithUndeclaredTypeVariables() throws IOException {
        testClassMember("javax.management", "StandardMBean", "StandardMBean-T-java.lang.Class-");
    }

    @Test
    public void testMethodWithArrayResultProtected() throws IOException {
        testClassMember("javax.management", "StandardMBean", "getConstructors-javax.management.MBeanConstructorInfo:A-java.lang.Object-");
    }

    @Test
    public void testProtectedConstructor() throws IOException {
        testClassMember("javax.management", "StandardMBean", "StandardMBean-java.lang.Class-");
    }

    @Test
    public void testMethodWithUnboundedWildcard() throws IOException {
        testClassMember("javax.management", "StandardMBean", "getImplementationClass--");
    }

    @Test
    public void testStaticMethod() throws IOException {
        testClassMember("java.lang", "String", "valueOf-java.lang.Object-");
    }

    @Test
    public void testStaticMethodWithVarArgs() throws IOException {
        testClassMember("java.lang", "String", "join-java.lang.CharSequence-java.lang.CharSequence...-");
    }

    @Test
    public void testStaticFinalField() throws IOException {
        testClassMember("java.lang", "String", "CASE_INSENSITIVE_ORDER");
    }

    @Test
    public void testProtectedField() throws IOException {
        testClassMember("java.util", "EventObject", "source");
    }

    @Test
    public void testAbstractMethod() throws IOException {
        testClassMember("java.util", "AbstractMap", "entrySet--");
    }

    @Test
    public void testInterfaceWithTypeVariablesWithoutSuperTypes() throws IOException {
        testClass("java.util", "Map");
    }

    @Test
    public void testClassWithInterconnectedTypeVariablesStatic() throws IOException {
        testClass("java.util", "Spliterator.OfPrimitive");
    }

    @Test
    public void testFinalClass() throws IOException {
        testClass("java.lang", "String");
    }

    @Test
    public void testAbstractClass() throws IOException {
        testClass("java.util", "AbstractMap");
    }

    @Test
    public void testEnum() throws IOException {
        testClass("java.util", "Formatter.BigDecimalLayoutForm");
    }

    @Test
    public void testAnnotation() throws IOException {
        testClass("java.lang", "SuppressWarnings");
    }

    @Test
    public void testClassWithBoundedTypeVariables() throws IOException {
        testClass("java.lang", "Enum");
    }

    @Test
    public void testPackageWithNestedClasses() throws IOException {
        testPackage("java.util.concurrent.locks");
    }

    @Test
    public void testPackageWithTwiceNestedClasses() throws IOException {
        testPackage("javax.swing");
    }

    @Test
    public void testAllEntities() throws IOException {
        for (PackageEntity pack : parser.getPackages().values()) {
            Assert.assertEquals(
                    FileUtils.readFileToString(new File(TEST_FOLDER + "global/" + pack.getName() + ".txt")),
                    pack.toString());
        }
    }

    @Test
    public void testPackageHierarchy() {
        for (PackageEntity pack : parser.getPackages().values()) {
            if (pack.getContainingPackage() != null) {
                Assert.assertTrue(pack.getContainingPackage().getSubPackages().contains(pack));
            }

            for (PackageEntity sub : pack.getSubPackages()) {
                Assert.assertSame(pack, sub.getContainingPackage());
            }
        }
    }

    @Test
    public void testPackageMembersContainingPackage() {
        for (PackageEntity pack : parser.getPackages().values()) {
            for (TypeConstructor type : pack.getContainedClasses()) {
                Assert.assertSame(pack, type.getContainingPackage());
            }
        }
    }

    @Test
    public void testClassPackageNotNull() {
        for (Classifier member : parser.getClasses().values()) {
            Assert.assertNotNull(member.getContainingPackage());
        }
    }

    @Test
    public void testClassPackageContainsClass() {
        for (Classifier member : parser.getClasses().values()) {
            Assert.assertTrue(member.getContainingPackage().getContainedClasses().contains(member));
        }
    }

    @Test
    public void testClassMembersContainingPackageNotNull() {
        for (Map<String, MemberEntity> members : parser.getClassMembers().values()) {
            for (MemberEntity member : members.values()) {
                Assert.assertNotNull(member.getContainingPackage());
            }
        }
    }

    @Test
    public void testClassMembersContainingTypeNotNull() {
        for (Map<String, MemberEntity> members : parser.getClassMembers().values()) {
            for (MemberEntity member : members.values()) {
                Assert.assertNotNull(member.getContainingType());
            }
        }
    }

    @Test
    public void testClassMembersContainingType() {
        for (Classifier packageMember : parser.getClasses().values()) {
            for (MemberEntity classMember : packageMember.getMembers()) {
                Assert.assertSame(packageMember, classMember.getContainingType());
            }
        }
    }

    @Test
    public void testClassMemberEnclosingClassContainsClassMember() {
        for (Map<String, MemberEntity> members : parser.getClassMembers().values()) {
            for (MemberEntity member : members.values()) {
                Assert.assertTrue(member.getContainingType().getMembers().contains(member));
            }
        }
    }

    @Test
    public void testClassMembersContainingPackage() {
        for (PackageEntity pack : parser.getPackages().values()) {
            for (TypeConstructor packageMember : pack.getContainedClasses()) {
                Classifier member = (Classifier) packageMember;
                for (MemberEntity classMember : member.getMembers()) {
                    Assert.assertSame(pack, classMember.getContainingPackage());
                }
            }
        }
    }

    @Test
    public void testClassMembersPackageSameAsClassPackage() {
        for (Classifier packageMember : parser.getClasses().values()) {
            for (MemberEntity classMember : packageMember.getMembers()) {
                Assert.assertSame(packageMember.getContainingPackage(), classMember.getContainingPackage());
            }
        }
    }

    private void testPackage(String packageName) throws IOException {
        Assert.assertEquals(
                FileUtils.readFileToString(new File(TEST_FOLDER + packageName + ".txt")),
                parser.getPackages().get(packageName).toString());
    }

    private void testClass(String packageName, String className) throws IOException {
        Assert.assertEquals(
                FileUtils.readFileToString(new File(TEST_FOLDER + packageName + "." + className + ".txt")),
                parser.getClasses().get(new QualifiedName(packageName, className)).toString());
    }

    private void testClassMember(String packageName, String className, String methodName) throws IOException {
        Assert.assertEquals(
                FileUtils.readFileToString(new File(TEST_FOLDER + packageName + "." + className + "." + methodName + ".txt")),
                parser.getClassMembers().get(new QualifiedName(packageName, className)).get(methodName).toString());
    }
}
