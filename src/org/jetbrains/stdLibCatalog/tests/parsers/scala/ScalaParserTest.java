package org.jetbrains.stdLibCatalog.tests.parsers.scala;

import org.apache.commons.io.FileUtils;
import org.jetbrains.stdLibCatalog.domain.*;
import org.jetbrains.stdLibCatalog.parsers.java.JavaParser;
import org.jetbrains.stdLibCatalog.parsers.scala.ScalaParser;
import org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils.QualifiedName;

public class ScalaParserTest {
    private final String TEST_FOLDER = "resources/tests/parsers/scala/";
    private static ScalaParser parser;

    @BeforeClass
    public static void setUp() throws IOException {
        JavaParser javaParser = new JavaParser();
        javaParser.parse();
        parser = new ScalaParser(javaParser);
        parser.parse();
    }

    @Test
    public void testFunctionWithMultipleParametersLists() throws IOException {
        testClassMember("scala.runtime", "Tuple3Zipped", "def aggregate[B](z: ⇒ B)(seqop: (B, (El1, El2, El3)) ⇒ B, combop: (B, B) ⇒ B): B");
    }

    @Test
    public void testFunctionWithImplicitParameters() throws IOException {
        testClassMember("scala.runtime", "Tuple3Zipped", "def unzip[A1, A2](implicit asPair: ((El1, El2, El3)) ⇒ (A1, A2)): (collection.Traversable[A1], collection.Traversable[A2])");
    }

    @Test
    public void testFunctionWithTypeParameters() throws IOException {
        testClassMember("scala.collection.generic", "SetFactory", "def setCanBuildFrom[A]: CanBuildFrom[CC[_], A, CC[A]]");
    }

    @Test
    public void testFunctionWithTypeParametersWithConstraints() throws IOException {
        testClassMember("scala.runtime", "Tuple3Zipped", "def scan[B >: A, That](z: B)(op: (B, B) ⇒ B)(implicit cbf: CanBuildFrom[collection.Traversable[(El1, El2, El3)], B, That]): That");
    }

    @Test
    public void testFunctionWithoutArguments() throws IOException {
        testClassMember("scala.collection.generic", "Clearable", "abstract def clear(): Unit");
    }

    @Test
    public void testFunctionWithoutArgumentsAndBraces() throws IOException {
        testClassMember("scala.collection", "BitSet", "def empty: BitSet");
    }

    @Test
    public void testFunctionWithCurriedResult() throws IOException {
        testClassMember("scala", "Function9", "def curried: (T1) ⇒ (T2) ⇒ (T3) ⇒ (T4) ⇒ (T5) ⇒ (T6) ⇒ (T7) ⇒ (T8) ⇒ (T9) ⇒ R");
    }

    @Test
    public void testFunctionWithColonInName() throws IOException {
        testClassMember("scala", "", "val #::: scala.collection.immutable.Stream.#::.type");
    }

    @Test
    public void testFieldOfNonExistingType() throws IOException {
        testClassMember("scala", "", "val Ordering: scala.math.Ordering.type");
    }

    @Test
    public void testFunctionWithDefaultParameters() throws IOException {
        testClassMember("scala.collection", "AbstractIterator", "def sliding[B >: A](size: Int, step: Int = 1): GroupedIterator[B]");
    }

    @Test
    public void testFunctionWithArgumentConstraints() throws IOException {
        testClassMember("scala.collection", "$plus$colon$", "def unapply[T, Coll <: SeqLike[T, Coll]](t: Coll with SeqLike[T, Coll]): Option[(T, Coll)]");
    }

    @Test
    public void testFieldWithValueSet() throws IOException {
        testClassMember("scala.math", "", "final val Pi: Double(3.141592653589793)");
    }

    @Test
    public void testClassesWithoutDocumentation() throws IOException {
        testClassMember("scala.util", "Either", "final def !=(arg0: Any): Boolean");
    }

    @Test
    public void testStarType() throws IOException {
        testClassMember("scala", "Enumeration$ValueSet$", "def apply(elems: Value*): ValueSet");
    }

    @Test
    public void testAliasToJavaClass() throws IOException {
        testAlias("scala", "Throwable");
    }

    @Test
    public void testAliasToInnerClass() throws IOException {
        testAlias("scala", "Enumeration$ValueSet$Self");
    }

    @Test
    public void testAliasWithParameters() throws IOException {
        testAlias("scala", "::");
    }

    @Test
    public void testClassWithCoContraVariantParameters() throws IOException {
        testClass("scala", "Function5");
    }

    @Test
    public void testClassWithComplexParametersConstraints() throws IOException {
        testClass("scala.collection.generic", "GenMapFactory");
    }

    @Test
    public void testClassWithConstructorParametersInDefinition() throws IOException {
        testClass("scala", "Tuple10");
    }

    @Test
    public void testClassExtendingFunctionalType() throws IOException {
        testClass("scala", "PartialFunction");
    }

    @Test
    public void testClassUsingOuterClassParameters() throws IOException {
        testClass("scala.collection.generic", "GenTraversableFactory$GenericCanBuildFrom");
    }

    @Test
    public void testClassAndObjectWithSameName() throws IOException {
        testClass("scala.math", "Ordering");
        testClass("scala.math", "Ordering$");
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
    public void testAliasPackageNotNull() {
        for (TypeAlias alias : parser.getAliases().values()) {
            Assert.assertNotNull(alias.getContainingPackage());
        }
    }

    @Test
    public void testClassPackageContainsClass() {
        for (Classifier member : parser.getClasses().values()) {
            Assert.assertTrue(member.getContainingPackage().getContainedClasses().contains(member));
        }
    }

    @Test
    public void testAliasPackageContainsClass() {
        for (TypeAlias alias : parser.getAliases().values()) {
            Assert.assertTrue(alias.getContainingPackage().getContainedClasses().contains(alias));
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
                if (member.getContainingType() != null) {
                    Assert.assertTrue(member.getContainingType().getMembers().contains(member));
                }
            }
        }
    }

    @Test
    public void testClassMembersContainingPackage() {
        for (PackageEntity pack : parser.getPackages().values()) {
            for (TypeConstructor packageMember : pack.getContainedClasses()) {
                if (packageMember instanceof Classifier) {
                    Classifier member = (Classifier) packageMember;
                    for (MemberEntity classMember : member.getMembers()) {
                        Assert.assertSame(pack, classMember.getContainingPackage());
                    }
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

    private void testClass(String packageName, String className) throws IOException {
        Assert.assertEquals(
                FileUtils.readFileToString(new File(TEST_FOLDER + packageName + "." + className + ".txt")),
                parser.getClasses().get(new QualifiedName(packageName, className)).toString());
    }

    private void testAlias(String packageName, String className) throws IOException {
        Assert.assertEquals(
                FileUtils.readFileToString(new File(TEST_FOLDER + packageName + "." + className + ".txt")),
                parser.getAliases().get(new QualifiedName(packageName, className)).toString());
    }

    private void testClassMember(String packageName, String className, String methodName) throws IOException {
        Assert.assertEquals(
                FileUtils.readFileToString(new File(TEST_FOLDER + packageName + "." + className + "." + methodName + ".txt")),
                parser.getClassMembers().get(new QualifiedName(packageName, className)).get(methodName).toString());
    }
}
