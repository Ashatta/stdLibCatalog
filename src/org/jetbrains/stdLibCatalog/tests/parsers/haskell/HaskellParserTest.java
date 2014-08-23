package org.jetbrains.stdLibCatalog.tests.parsers.haskell;

import org.apache.commons.io.FileUtils;
import org.jetbrains.stdLibCatalog.domain.*;
import org.jetbrains.stdLibCatalog.parsers.haskell.HaskellParser;
import org.jetbrains.stdLibCatalog.parsers.utils.ParserUtils;
import org.jsoup.helper.StringUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HaskellParserTest {
    private final String TEST_FOLDER = "resources/tests/parsers/haskell/";
    private static HaskellParser parser;

    @BeforeClass
    public static void setUp() throws IOException {
        parser = new HaskellParser();
        parser.parse();
    }

    @Test
    public void testPackageEmpty() throws IOException {
        testPackage("Bits");
    }

    @Test
    public void testPackageWithoutSynopsis() throws IOException {
        testPackage("Distribution.Text");
    }

    @Test
    public void testPackageTuples() throws IOException {
        testPackage("GHC.Tuple");
    }

    @Test
    public void testTypeclassWithSplitMethod() throws IOException {
        testClassifier("Compiler.Hoopl", "IfThenElseable");
    }

    @Test
    public void testTypeclassWithInfixMethod() throws IOException {
        testClassifier("Control.Category", "Category");
    }

    @Test
    public void testTypeclassWithMultipleParents() throws IOException {
        testClassifier("Control.Exception", "Exception");
    }

    @Test
    public void testTypeclassWithSingleParent() throws IOException {
        testClassifier("Control.Monad", "MonadPlus");
    }

    @Test
    public void testTypeclassWithInfixDataInMethods() throws IOException {
        testClassifier("Data.Type.Equality", "TestEquality");
    }

    @Test
    public void testTypeclassWithManyMethodsAndInstances() throws IOException {
        testClassifier("Data.Data", "Data");
    }

    @Test
    public void testDataWithKindsWithoutConstructor() throws IOException {
        testClassifier("Array", "Array");
    }

    @Test
    public void testDataWithComplexConstructors() throws IOException {
        testClassifier("Compiler.Hoopl", "Graph'");
    }

    @Test
    public void testDataWithInfixConstructor() throws IOException {
        testClassifier("Complex", "Complex");
    }

    @Test
    public void testDataEnumLike() throws IOException {
        testClassifier("Control.Exception", "ArithException");
    }

    @Test
    public void testDataWithMultipleConstructors() throws IOException {
        testClassifier("Data.Binary.Get", "Decoder");
    }

    @Test
    public void testDataWithMultipleConstructorsAndVariables() throws IOException {
        testClassifier("Data.Either", "Either");
    }

    @Test
    public void testDataInfixDefinition() throws IOException {
        testClassifier("Data.Type.Equality", "(:~:)");
    }

    @Test
    public void testDataWithFields() throws IOException {
        testClassifier("Distribution.InstalledPackageInfo", "InstalledPackageInfo_");
    }

    @Test
    public void testNewtypeWithVariables() throws IOException {
        testClassifier("Data.Functor.Compose", "Compose");
    }

    @Test
    public void testSimpleAliasWithoutParameters() throws IOException {
        testAlias("Char", "String");
    }

    @Test
    public void testAliasWithParameters() throws IOException {
        testAlias("Distribution.Compat.ReadP", "ReadP");
    }

    @Test
    public void testAliasForComplexFunctionType() throws IOException {
        testAlias("Distribution.Compat.ReadP", "ReadS");
    }

    @Test
    public void testAliasForFunctionWithForall() throws IOException {
        testAlias("Compiler.Hoopl", "TraceFn");
    }

    @Test
    public void testAliasWithInfixTypes() throws IOException {
        testAlias("GHC.TypeLits", "(<=)");
    }

    @Test
    public void testFunctionSplitInMultipleLines() throws IOException {
        testFunction("Array", "accumArray");
    }

    @Test
    public void testInfixFunction() throws IOException {
        testFunction("Array", "(!)");
    }

    @Test
    public void testFunctionWithoutParametersAndTypeVariables() throws IOException {
        testFunction("CPUTime", "getCPUTime");
    }

    @Test
    public void testSimpleUnaryFunctionWithourTypeVariables() throws IOException {
        testFunction("Char", "intToDigit");
    }

    @Test
    public void testFunctionWithComplexTypesAndConstraints() throws IOException {
        testFunction("Compiler.Hoopl", "bodyList");
    }

    @Test
    public void testFunctionWithApostrophesAndForalls() throws IOException {
        testFunction("Compiler.Hoopl", "mapGraphBlocks");
    }

    @Test
    public void testFunctionWithMultipleConstraintsAndForalls() throws IOException {
        testFunction("Data.Data", "fromConstrM");
    }

    @Test
    public void testFunctionWithKParametersAndConstraints() throws IOException {
        testFunction("Data.Map", "insertWithKey'");
    }

    @Test
    public void testFunctionWithInfixData() throws IOException {
        testFunction("Data.Type.Equality", "apply");
    }

    @Test
    public void testFunctionWithUnboxedTypes() throws IOException {
        testFunction("GHC.Prim", "casMutVar#");
    }

    @Test
    public void testFakeInstanceClassifiers() throws IOException {
        testClassifier("Data.Type.Equality", "Data.Data.Data_(:~:)<a<>,b<>>");
        testClassifier("Control.Applicative", "Data.Typeable.Internal.Typeable_Applicative<>");
        testClassifier("Data.Type.Coercion", "Data.Typeable.Internal.Typeable_Coercion<>");
        testClassifier("other", "Data.Functor.Functor_(->)<r<>>");
    }

    @Test
    public void testPackage() {
        PackageEntity pack = parser.getPackages().get("Compiler.Hoopl");
        testPackageChilden(pack, "Compiler.Hoopl.Internals,Passes,Compiler.Hoopl.Wrappers");

        PackageEntity parent = parser.getPackages().get("Compiler");
        Assert.assertSame(parent, pack.getContainingPackage());
        Assert.assertTrue(parent.getSubPackages().contains(pack));
    }

    @Test
    public void testTopLevelPackage() {
        PackageEntity pack = parser.getPackages().get("Control");
        testPackageChilden(pack, "Control.Applicative,Control.Arrow,Control.Category,Control.Concurrent,Control.DeepSeq,Control.Exception,Control.Monad");
        Assert.assertNull(pack.getContainingPackage());
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
            Assert.assertTrue(member.getAttr("fakeInstanceClassifier").equals("true")
                    || member.getContainingPackage().getContainedClasses().contains(member));
        }
    }

    @Test
    public void testAliasPackageNotNull() {
        for (TypeAlias alias : parser.getAliases().values()) {
            Assert.assertNotNull(alias.getContainingPackage());
        }
    }

    @Test
    public void testAliasPackageContainsAlias() {
        for (TypeAlias alias : parser.getAliases().values()) {
            Assert.assertTrue(alias.getContainingPackage().getContainedClasses().contains(alias));
        }
    }

    @Test
    public void testFunctionPackageNotNull() {
        for (MemberEntity function : parser.getFunctions().values()) {
            Assert.assertNotNull(function.getContainingPackage());
        }
    }

    @Test
    public void testFunctionPackageContainsFunction() {
        for (MemberEntity function : parser.getFunctions().values()) {
            Assert.assertTrue(function.getContainingPackage().getMembers().contains(function));
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
        for (MemberEntity member : parser.getFunctions().values()) {
            if (member.getContainingType() != null) {
                Assert.assertTrue(member.getContainingType().getMembers().contains(member));
            }
        }
    }

    @Test
    public void testClassMembersContainingPackage() {
        for (PackageEntity pack : parser.getPackages().values()) {
            for (TypeConstructor packageMember : pack.getContainedClasses()) {
                try {
                    Classifier member = (Classifier) packageMember;  // could be TypeAlias
                    for (MemberEntity classMember : member.getMembers()) {
                        Assert.assertSame(pack, classMember.getContainingPackage());
                    }
                } catch (ClassCastException ignored) {
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
        Assert.assertEquals(FileUtils.readFileToString(new File(TEST_FOLDER + packageName + ".txt")),
                parser.getPackages().get(packageName).toString());
    }

    private void testClassifier(String packageName, String entityName) throws IOException {
        Assert.assertEquals(FileUtils.readFileToString(new File(TEST_FOLDER + packageName + "." + entityName + ".txt")),
                parser.getClasses().get(new ParserUtils.QualifiedName(packageName, entityName)).toString());
    }

    private void testFunction(String packageName, String entityName) throws IOException {
        Assert.assertEquals(FileUtils.readFileToString(new File(TEST_FOLDER + packageName + "." + entityName + ".txt")),
                parser.getFunctions().get(new ParserUtils.QualifiedName(packageName, entityName)).toString());
    }

    private void testAlias(String packageName, String entityName) throws IOException {
        Assert.assertEquals(
                FileUtils.readFileToString(new File(TEST_FOLDER + packageName + "." + entityName + ".txt")),
                parser.getAliases().get(new ParserUtils.QualifiedName(packageName, entityName)).toString());
    }

    public void testPackageChilden(PackageEntity pack, String expectedChildrenString) {
        List<String> children = new ArrayList<>();
        for (PackageEntity subPackage : pack.getSubPackages()) {
            children.add(subPackage.getName());
            Assert.assertSame(pack, subPackage.getContainingPackage());
        }

        Assert.assertEquals(expectedChildrenString, StringUtil.join(children, ","));
    }
}
