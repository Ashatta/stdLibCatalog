package org.jetbrains.stdLibCatalog.tests.parsers.haskell;

import org.apache.commons.io.FileUtils;
import org.jetbrains.stdLibCatalog.domain.PackageEntity;
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
