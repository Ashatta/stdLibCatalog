package org.jetbrains.stdLibCatalog.tests.parsers.haskell;

import org.apache.commons.io.FileUtils;
import org.jetbrains.stdLibCatalog.domain.PackageEntity;
import org.jetbrains.stdLibCatalog.parsers.haskell.HaskellParser;
import org.jsoup.helper.StringUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HaskellParserTest {
    private static HaskellParser PARSER;

    private enum EntityType {
        PACKAGE,
        TYPECLASS,
        DATA,
        NEWTYPE,
        ALIAS,
        FUNCTION
    }

    @BeforeClass
    public static void setUp() throws IOException {
        PARSER = new HaskellParser();
        PARSER.parse();
    }

    @Test
    public void testPackageEmpty() throws IOException {
        test(EntityType.PACKAGE, "Bits", "Bits");
    }

    @Test
    public void testPackageWithoutSynopsis() throws IOException {
        test(EntityType.PACKAGE, "Distribution.Text", "Distribution.Text");
    }

    @Test
    public void testPackageTuples() throws IOException {
        test(EntityType.PACKAGE, "GHC.Tuple", "GHC.Tuple");
    }

    @Test
    public void testTypeclassWithSplitMethod() throws IOException {
        test(EntityType.TYPECLASS, "Compiler.Hoopl", "IfThenElseable");
    }

    @Test
    public void testTypeclassWithInfixMethod() throws IOException {
        test(EntityType.TYPECLASS, "Control.Category", "Category");
    }

    @Test
    public void testTypeclassWithMultipleParents() throws IOException {
        test(EntityType.TYPECLASS, "Control.Exception", "Exception");
    }

    @Test
    public void testTypeclassWithSingleParent() throws IOException {
        test(EntityType.TYPECLASS, "Control.Monad", "MonadPlus");
    }

    @Test
    public void testTypeclassWithInfixDataInMethods() throws IOException {
        test(EntityType.TYPECLASS, "Data.Type.Equality", "TestEquality");
    }

    @Test
    public void testTypeclassWithManyMethodsAndInstances() throws IOException {
        test(EntityType.TYPECLASS, "Data.Data", "Data");
    }

    @Test
    public void testDataWithKindsWithoutConstructor() throws IOException {
        test(EntityType.DATA, "Array", "Array");
    }

    @Test
    public void testDataWithComplexConstructors() throws IOException {
        test(EntityType.DATA, "Compiler.Hoopl", "Graph'");
    }

    @Test
    public void testDataWithInfixConstructor() throws IOException {
        test(EntityType.DATA, "Complex", "Complex");
    }

    @Test
    public void testDataEnumLike() throws IOException {
        test(EntityType.DATA, "Control.Exception", "ArithException");
    }

    @Test
    public void testDataWithMultipleConstructors() throws IOException {
        test(EntityType.DATA, "Data.Binary.Get", "Decoder");
    }

    @Test
    public void testDataWithMultipleConstructorsAndVariables() throws IOException {
        test(EntityType.DATA, "Data.Either", "Either");
    }

    @Test
    public void testDataInfixDefinition() throws IOException {
        test(EntityType.DATA, "Data.Type.Equality", "(:~:)");
    }

    @Test
    public void testDataWithFields() throws IOException {
        test(EntityType.DATA, "Distribution.InstalledPackageInfo", "InstalledPackageInfo_");
    }

    @Test
    public void testNewtypeWithVariables() throws IOException {
        test(EntityType.NEWTYPE, "Data.Functor.Compose", "Compose");
    }

    @Test
    public void testSimpleAliasWithoutParameters() throws IOException {
        test(EntityType.ALIAS, "Char", "String");
    }

    @Test
    public void testAliasWithParameters() throws IOException {
        test(EntityType.ALIAS, "Distribution.Compat.ReadP", "ReadP");
    }

    @Test
    public void testAliasForComplexFunctionType() throws IOException {
        test(EntityType.ALIAS, "Distribution.Compat.ReadP", "ReadS");
    }

    @Test
    public void testAliasForFunctionWithForall() throws IOException {
        test(EntityType.ALIAS, "Compiler.Hoopl", "TraceFn");
    }

    @Test
    public void testAliasWithInfixTypes() throws IOException {
        test(EntityType.ALIAS, "GHC.TypeLits", "(<=)");
    }

    @Test
    public void testFunctionSplitInMultipleLines() throws IOException {
        test(EntityType.FUNCTION, "Array", "accumArray");
    }

    @Test
    public void testInfixFunction() throws IOException {
        test(EntityType.FUNCTION, "Array", "(!)");
    }

    @Test
    public void testFunctionWithoutParametersAndTypeVariables() throws IOException {
        test(EntityType.FUNCTION, "CPUTime", "getCPUTime");
    }

    @Test
    public void testSimpleUnaryFunctionWithourTypeVariables() throws IOException {
        test(EntityType.FUNCTION, "Char", "intToDigit");
    }

    @Test
    public void testFunctionWithComplexTypesAndConstraints() throws IOException {
        test(EntityType.FUNCTION, "Compiler.Hoopl", "bodyList");
    }

    @Test
    public void testFunctionWithApostrophesAndForalls() throws IOException {
        test(EntityType.FUNCTION, "Compiler.Hoopl", "mapGraphBlocks");
    }

    @Test
    public void testFunctionWithMultipleConstraintsAndForalls() throws IOException {
        test(EntityType.FUNCTION, "Data.Data", "fromConstrM");
    }

    @Test
    public void testFunctionWithKParametersAndConstraints() throws IOException {
        test(EntityType.FUNCTION, "Data.Map", "insertWithKey'");
    }

    @Test
    public void testFunctionWithInfixData() throws IOException {
        test(EntityType.FUNCTION, "Data.Type.Equality", "apply");
    }

    @Test
    public void testFunctionWithUnboxedTypes() throws IOException {
        test(EntityType.FUNCTION, "GHC.Prim", "casMutVar#");
    }

    @Test
    public void testFakeInstanceClassifiers() throws IOException {
        test(EntityType.DATA, "Data.Type.Equality", "Data.Data.Data_(:~:)<a<>,b<>>");
        test(EntityType.DATA, "Control.Applicative", "Data.Typeable.Internal.Typeable_Applicative<>");
        test(EntityType.DATA, "Data.Type.Coercion", "Data.Typeable.Internal.Typeable_Coercion<>");
        test(EntityType.DATA, "other", "Data.Functor.Functor_(->)<r<>>");
    }

    @Test
    public void testPackage() {
        PackageEntity pack = PARSER.getPackages().get("Compiler.Hoopl");
        testPackageChilden(pack, "Compiler.Hoopl.Internals,Passes,Compiler.Hoopl.Wrappers");

        PackageEntity parent = PARSER.getPackages().get("Compiler");
        Assert.assertSame(parent, pack.getContainingPackage());
        Assert.assertTrue(parent.getSubPackages().contains(pack));
    }

    @Test
    public void testTopLevelPackage() {
        PackageEntity pack = PARSER.getPackages().get("Control");
        testPackageChilden(pack, "Control.Applicative,Control.Arrow,Control.Category,Control.Concurrent,Control.DeepSeq,Control.Exception,Control.Monad");
        Assert.assertNull(pack.getContainingPackage());
    }

    private void test(EntityType type, String packageName, String entityName) throws IOException {
        final String TEST_FOLDER = "src/org/jetbrains/stdLibCatalog/tests/parsers/haskell/files/";

        switch (type) {
            case PACKAGE:
                Assert.assertEquals(FileUtils.readFileToString(new File(TEST_FOLDER + packageName + ".txt")),
                    PARSER.getPackages().get(packageName).toString());
                break;
            case TYPECLASS:
            case DATA:
            case NEWTYPE: Assert.assertEquals(
                    FileUtils.readFileToString(new File(TEST_FOLDER + packageName + "." + entityName + ".txt")),
                    PARSER.getClasses().get(new HaskellParser.QualifiedName(packageName, entityName)).toString());
                break;
            case ALIAS: Assert.assertEquals(
                    FileUtils.readFileToString(new File(TEST_FOLDER + packageName + "." + entityName + ".txt")),
                    PARSER.getAliases().get(new HaskellParser.QualifiedName(packageName, entityName)).toString());
                break;
            case FUNCTION: Assert.assertEquals(
                    FileUtils.readFileToString(new File(TEST_FOLDER + packageName + "." + entityName + ".txt")),
                    PARSER.getFunctions().get(new HaskellParser.QualifiedName(packageName, entityName)).toString());
        }
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
