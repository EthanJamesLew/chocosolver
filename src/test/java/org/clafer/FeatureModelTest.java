package org.clafer;

import org.clafer.ast.AstAbstractClafer;
import org.clafer.ast.AstConcreteClafer;
import org.clafer.ast.AstModel;
import static org.clafer.ast.Asts.*;
import org.clafer.compiler.ClaferCompiler;
import org.clafer.compiler.ClaferOptimizer;
import org.clafer.objective.Objective;
import org.clafer.scope.Scope;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author jimmy
 */
public class FeatureModelTest {

    @Test(timeout = 60000)
    public void testSmallFeatureModel() {
        AstModel model = newModel();

        AstAbstractClafer feature = model.addAbstract("Feature");
        AstConcreteClafer footprint = feature.addChild("Footprint").withCard(1, 1).refTo(IntType);

        int n = 50;
        for (int i = 0; i < n; i++) {
            AstConcreteClafer f = model.addChild("f" + i).withCard(0, 1).extending(feature);
            f.addConstraint(equal(joinRef(join($this(), footprint)), constant(n - i * 2 + 1)));
        }

        ClaferOptimizer solver = ClaferCompiler.compile(model, Scope.defaultScope(100).intLow(-1000).intHigh(1000),
                Objective.minimize(sum(global(footprint))));
        assertTrue(solver.find());
        assertArrayEquals(new int[]{-576}, solver.optimalValues());
    }

    /**
     * Adapted from Scalable Prediction of Non-functional Properties in Software
     * Product Lines. Scaled down by dividing numbers by 2000 and rounding.
     *
     * <pre>
     * abstract IMeasurable
     *     footprint : integer
     *
     * abstract SQLite
     *     OperatingSystemCharacteristics : IMeasurable
     *         [ this.footprint = 888]
     *         SQLITE_4_BYTE_ALIGNED_MALLOC : IMeasurable
     *             [ this.footprint = 0]
     *         SQLITE_CASE_SENSITIVE_LIKE : IMeasurable  ?
     *             [ this.footprint = 0]
     *         SQLITE_HAVE_ISNAN : IMeasurable  ?
     *             [ this.footprint = 0]
     *         SQLITE_SECURE_DELETE : IMeasurable  ?
     *             [ this.footprint = 0]
     *         xor ChooseSQLITE_TEMP_STORE : IMeasurable  ?
     *             [ this.footprint = 0]
     *             SQLITE_TEMP_STORE_EQ_0 : IMeasurable
     *                 [ this.footprint = 0]
     *             SQLITE_TEMP_STORE_EQ_1 : IMeasurable
     *                 [ this.footprint = 0]
     *             SQLITE_TEMP_STORE_EQ_2 : IMeasurable
     *                 [ this.footprint = 0]
     *             SQLITE_TEMP_STORE_EQ_3 : IMeasurable
     *                 [ this.footprint = 0]
     *     EnableFeatures : IMeasurable
     *         [ this.footprint = 0]
     *         SQLITE_ENABLE_ATOMIC_WRITE : IMeasurable  ?
     *             [ this.footprint = 3]
     *         SQLITE_ENABLE_COLUMN_METADATA : IMeasurable  ?
     *             [ !SQLITE_OMIT_DECLTYPE ]
     *             [ this.footprint = 2]
     *         SQLITE_ENABLE_FTS3 : IMeasurable  ?
     *             [ this.footprint = 100]
     *         SQLITE_ENABLE_FTS3_PARENTHESIS : IMeasurable  ?
     *             [ this.footprint = 0]
     *         SQLITE_ENABLE_MEMORY_MANAGEMENT : IMeasurable  ?
     *             [ this.footprint = 1]
     *         xor ChooseMemSys : IMeasurable  ?
     *             [ this.footprint = 3]
     *             SQLITE_ENABLE_MEMSYS3 : IMeasurable
     *                 [ this.footprint = 2]
     *             SQLITE_ENABLE_MEMSYS5 : IMeasurable
     *                 [ this.footprint = 0]
     *         SQLITE_ENABLE_RTREE : IMeasurable  ?
     *             [ this.footprint = 33]
     *         SQLITE_ENABLE_STAT2 : IMeasurable  ?
     *             [ this.footprint = 4]
     *         SQLITE_ENABLE_UPDATE_DELETE_LIMIT : IMeasurable  ?
     *             [ this.footprint = 1]
     *         SQLITE_ENABLE_UNLOCK_NOTIFY : IMeasurable  ?
     *             [ this.footprint = 3]
     *         SQLITE_SOUNDEX : IMeasurable  ?
     *             [ this.footprint = 1]
     *         YYTRACKMAXSTACKDEPTH : IMeasurable  ?
     *             [ this.footprint = 0]
     *     DisableFeatures : IMeasurable
     *         [ this.footprint = 0]
     *         SQLITE_DISABLE_LFS : IMeasurable  ?
     *             [ this.footprint = 0]
     *         SQLITE_DISABLE_DIRSYNC : IMeasurable  ?
     *             [ this.footprint = 0]
     *     OmitFeatures : IMeasurable
     *         [ this.footprint = 0]
     *         SQLITE_OMIT_ALTERTABLE : IMeasurable  ?
     *             [ this.footprint = -7]
     *         SQLITE_OMIT_ANALYZE : IMeasurable  ?
     *             [ this.footprint = -5]
     *         SQLITE_OMIT_ATTACH : IMeasurable  ?
     *             [ this.footprint = -8]
     *         SQLITE_OMIT_AUTHORIZATION : IMeasurable  ?
     *             [ this.footprint = -5]
     *         SQLITE_OMIT_AUTOINCREMENT : IMeasurable  ?
     *             [ this.footprint = -3]
     *         SQLITE_OMIT_AUTOMATIC_INDEX : IMeasurable  ?
     *             [ this.footprint = -4]
     *         SQLITE_OMIT_AUTOINIT : IMeasurable  ?
     *             [ this.footprint = 0]
     *         SQLITE_OMIT_AUTOVACUUM : IMeasurable  ?
     *             [ this.footprint = -14]
     *         SQLITE_OMIT_BETWEEN_OPTIMIZATION : IMeasurable  ?
     *             [ this.footprint = 0]
     *         SQLITE_OMIT_BLOB_LITERAL : IMeasurable  ?
     *             [ this.footprint = -1]
     *         SQLITE_OMIT_BTREECOUNT : IMeasurable  ?
     *             [ this.footprint = -2]
     *         SQLITE_OMIT_BUILTIN_TEST : IMeasurable  ?
     *             [ this.footprint = -2]
     *         SQLITE_OMIT_CAST : IMeasurable  ?
     *             [ this.footprint = -1]
     *         SQLITE_OMIT_CHECK : IMeasurable  ?
     *             [ this.footprint = -1]
     *         SQLITE_OMIT_COMPILEOPTION_DIAGS : IMeasurable  ?
     *             [ this.footprint = -1]
     *         SQLITE_OMIT_COMPLETE : IMeasurable  ?
     *             [ this.footprint = -888]
     *         SQLITE_OMIT_COMPOUND_SELECT : IMeasurable  ?
     *             [ this.footprint = -10]
     *         SQLITE_OMIT_CONFLICT_CLAUSE : IMeasurable  ?
     *             [ this.footprint = 0]
     *         SQLITE_OMIT_DATETIME_FUNCS : IMeasurable  ?
     *             [ this.footprint = -10]
     *         SQLITE_OMIT_DECLTYPE : IMeasurable  ?
     *             [ !SQLITE_ENABLE_COLUMN_METADATA ]
     *             [ this.footprint = -1]
     *         SQLITE_OMIT_DEPRECATED : IMeasurable  ?
     *             [ this.footprint = -1]
     *         SQLITE_OMIT_EXPLAIN : IMeasurable  ?
     *             [ this.footprint = -10]
     *         SQLITE_OMIT_FLAG_PRAGMAS : IMeasurable  ?
     *             [ this.footprint = -1]
     *         SQLITE_OMIT_FLOATING_POINT : IMeasurable  ?
     *             [ this.footprint = -18]
     *         SQLITE_OMIT_FOREIGN_KEY : IMeasurable  ?
     *             [ this.footprint = -15]
     *         SQLITE_OMIT_GET_TABLE : IMeasurable  ?
     *             [ this.footprint = -3]
     *         SQLITE_OMIT_INCRBLOB : IMeasurable  ?
     *             [ this.footprint = -7]
     *         SQLITE_OMIT_INTEGRITY_CHECK : IMeasurable  ?
     *             [ this.footprint = -9]
     *         SQLITE_OMIT_LIKE_OPTIMIZATION : IMeasurable  ?
     *             [ this.footprint = -2]
     *         SQLITE_OMIT_LOAD_EXTENSION : IMeasurable  ?
     *             [ this.footprint = -3]
     *         SQLITE_OMIT_LOCALTIME : IMeasurable  ?
     *             [ this.footprint = -1]
     *         SQLITE_OMIT_LOOKASIDE : IMeasurable  ?
     *             [ this.footprint = -1]
     *         SQLITE_OMIT_MEMORYDB : IMeasurable  ?
     *             [ this.footprint = -2]
     *         SQLITE_OMIT_OR_OPTIMIZATION : IMeasurable  ?
     *             [ this.footprint = -5]
     *         SQLITE_OMIT_PAGER_PRAGMAS : IMeasurable  ?
     *             [ this.footprint = -5]
     *         SQLITE_OMIT_PRAGMA : IMeasurable  ?
     *             [ this.footprint = -16]
     *         SQLITE_OMIT_PROGRESS_CALLBACK : IMeasurable  ?
     *             [ this.footprint = -1]
     *         SQLITE_OMIT_QUICKBALANCE : IMeasurable  ?
     *             [ this.footprint = -1]
     *         SQLITE_OMIT_REINDEX : IMeasurable  ?
     *             [ this.footprint = -1]
     *         SQLITE_OMIT_SCHEMA_PRAGMAS : IMeasurable  ?
     *             [ this.footprint = -2]
     *         SQLITE_OMIT_SCHEMA_VERSION_PRAGMAS : IMeasurable  ?
     *             [ this.footprint = -1]
     *         SQLITE_OMIT_SHARED_CACHE : IMeasurable  ?
     *             [ this.footprint = -10]
     *         SQLITE_OMIT_SUBQUERY : IMeasurable  ?
     *             [ this.footprint = -11]
     *         SQLITE_OMIT_TCL_VARIABLE : IMeasurable  ?
     *             [ this.footprint = 0]
     *         SQLITE_OMIT_TEMPDB : IMeasurable  ?
     *             [ this.footprint = 0]
     *         SQLITE_OMIT_TRACE : IMeasurable  ?
     *             [ this.footprint = -3]
     *         SQLITE_OMIT_TRIGGER : IMeasurable  ?
     *             [ this.footprint = -27]
     *         SQLITE_OMIT_TRUNCATE_OPTIMIZATION : IMeasurable  ?
     *             [ this.footprint = 0]
     *         SQLITE_OMIT_UTF16 : IMeasurable  ?
     *             [ this.footprint = -8]
     *         SQLITE_OMIT_VACUUM : IMeasurable  ?
     *             [ this.footprint = -4]
     *         SQLITE_OMIT_VIEW : IMeasurable  ?
     *             [ this.footprint = -3]
     *         SQLITE_OMIT_VIRTUALTABLE : IMeasurable  ?
     *             [ this.footprint = -20]
     *         SQLITE_OMIT_WAL : IMeasurable  ?
     *             [ this.footprint = -30]
     *         SQLITE_OMIT_XFER_OPT : IMeasurable  ?
     *             [ this.footprint = -3]
     *     SQLITE_DEBUG : IMeasurable  ?
     *         [ this.footprint = 9]
     *     SQLITE_MEMDEBUG : IMeasurable  ?
     *         [ this.footprint = 2]
     *
     * simpleConfig : SQLite
     *
     * << min sum(footprint) >>
     * </pre>
     */
    @Test(timeout = 60000)
    public void testSqlLite() {
        AstModel model = newModel();
        AstAbstractClafer c3_SQLite = model.addAbstract("c3_SQLite");
        AstAbstractClafer c1_IMeasurable = model.addAbstract("c1_IMeasurable");
        AstConcreteClafer c2_footprint = c1_IMeasurable.addChild("c2_footprint").withCard(1, 1);
        AstConcreteClafer c4_OperatingSystemCharacteristics = c3_SQLite.addChild("c4_OperatingSystemCharacteristics").withCard(1, 1).extending(c1_IMeasurable);
        AstConcreteClafer c10_SQLITE_4_BYTE_ALIGNED_MALLOC = c4_OperatingSystemCharacteristics.addChild("c10_SQLITE_4_BYTE_ALIGNED_MALLOC").withCard(1, 1).extending(c1_IMeasurable);
        AstConcreteClafer c16_SQLITE_CASE_SENSITIVE_LIKE = c4_OperatingSystemCharacteristics.addChild("c16_SQLITE_CASE_SENSITIVE_LIKE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c22_SQLITE_HAVE_ISNAN = c4_OperatingSystemCharacteristics.addChild("c22_SQLITE_HAVE_ISNAN").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c28_SQLITE_SECURE_DELETE = c4_OperatingSystemCharacteristics.addChild("c28_SQLITE_SECURE_DELETE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c34_ChooseSQLITE_TEMP_STORE = c4_OperatingSystemCharacteristics.addChild("c34_ChooseSQLITE_TEMP_STORE").withCard(0, 1).withGroupCard(1, 1).extending(c1_IMeasurable);
        AstConcreteClafer c40_SQLITE_TEMP_STORE_EQ_0 = c34_ChooseSQLITE_TEMP_STORE.addChild("c40_SQLITE_TEMP_STORE_EQ_0").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c46_SQLITE_TEMP_STORE_EQ_1 = c34_ChooseSQLITE_TEMP_STORE.addChild("c46_SQLITE_TEMP_STORE_EQ_1").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c52_SQLITE_TEMP_STORE_EQ_2 = c34_ChooseSQLITE_TEMP_STORE.addChild("c52_SQLITE_TEMP_STORE_EQ_2").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c58_SQLITE_TEMP_STORE_EQ_3 = c34_ChooseSQLITE_TEMP_STORE.addChild("c58_SQLITE_TEMP_STORE_EQ_3").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c64_EnableFeatures = c3_SQLite.addChild("c64_EnableFeatures").withCard(1, 1).extending(c1_IMeasurable);
        AstConcreteClafer c70_SQLITE_ENABLE_ATOMIC_WRITE = c64_EnableFeatures.addChild("c70_SQLITE_ENABLE_ATOMIC_WRITE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c76_SQLITE_ENABLE_COLUMN_METADATA = c64_EnableFeatures.addChild("c76_SQLITE_ENABLE_COLUMN_METADATA").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c84_SQLITE_ENABLE_FTS3 = c64_EnableFeatures.addChild("c84_SQLITE_ENABLE_FTS3").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c90_SQLITE_ENABLE_FTS3_PARENTHESIS = c64_EnableFeatures.addChild("c90_SQLITE_ENABLE_FTS3_PARENTHESIS").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c96_SQLITE_ENABLE_MEMORY_MANAGEMENT = c64_EnableFeatures.addChild("c96_SQLITE_ENABLE_MEMORY_MANAGEMENT").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c102_ChooseMemSys = c64_EnableFeatures.addChild("c102_ChooseMemSys").withCard(0, 1).withGroupCard(1, 1).extending(c1_IMeasurable);
        AstConcreteClafer c108_SQLITE_ENABLE_MEMSYS3 = c102_ChooseMemSys.addChild("c108_SQLITE_ENABLE_MEMSYS3").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c114_SQLITE_ENABLE_MEMSYS5 = c102_ChooseMemSys.addChild("c114_SQLITE_ENABLE_MEMSYS5").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c120_SQLITE_ENABLE_RTREE = c64_EnableFeatures.addChild("c120_SQLITE_ENABLE_RTREE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c126_SQLITE_ENABLE_STAT2 = c64_EnableFeatures.addChild("c126_SQLITE_ENABLE_STAT2").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c132_SQLITE_ENABLE_UPDATE_DELETE_LIMIT = c64_EnableFeatures.addChild("c132_SQLITE_ENABLE_UPDATE_DELETE_LIMIT").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c138_SQLITE_ENABLE_UNLOCK_NOTIFY = c64_EnableFeatures.addChild("c138_SQLITE_ENABLE_UNLOCK_NOTIFY").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c144_SQLITE_SOUNDEX = c64_EnableFeatures.addChild("c144_SQLITE_SOUNDEX").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c150_YYTRACKMAXSTACKDEPTH = c64_EnableFeatures.addChild("c150_YYTRACKMAXSTACKDEPTH").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c156_DisableFeatures = c3_SQLite.addChild("c156_DisableFeatures").withCard(1, 1).extending(c1_IMeasurable);
        AstConcreteClafer c162_SQLITE_DISABLE_LFS = c156_DisableFeatures.addChild("c162_SQLITE_DISABLE_LFS").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c168_SQLITE_DISABLE_DIRSYNC = c156_DisableFeatures.addChild("c168_SQLITE_DISABLE_DIRSYNC").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c174_OmitFeatures = c3_SQLite.addChild("c174_OmitFeatures").withCard(1, 1).extending(c1_IMeasurable);
        AstConcreteClafer c180_SQLITE_OMIT_ALTERTABLE = c174_OmitFeatures.addChild("c180_SQLITE_OMIT_ALTERTABLE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c187_SQLITE_OMIT_ANALYZE = c174_OmitFeatures.addChild("c187_SQLITE_OMIT_ANALYZE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c194_SQLITE_OMIT_ATTACH = c174_OmitFeatures.addChild("c194_SQLITE_OMIT_ATTACH").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c201_SQLITE_OMIT_AUTHORIZATION = c174_OmitFeatures.addChild("c201_SQLITE_OMIT_AUTHORIZATION").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c208_SQLITE_OMIT_AUTOINCREMENT = c174_OmitFeatures.addChild("c208_SQLITE_OMIT_AUTOINCREMENT").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c215_SQLITE_OMIT_AUTOMATIC_INDEX = c174_OmitFeatures.addChild("c215_SQLITE_OMIT_AUTOMATIC_INDEX").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c222_SQLITE_OMIT_AUTOINIT = c174_OmitFeatures.addChild("c222_SQLITE_OMIT_AUTOINIT").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c228_SQLITE_OMIT_AUTOVACUUM = c174_OmitFeatures.addChild("c228_SQLITE_OMIT_AUTOVACUUM").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c235_SQLITE_OMIT_BETWEEN_OPTIMIZATION = c174_OmitFeatures.addChild("c235_SQLITE_OMIT_BETWEEN_OPTIMIZATION").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c241_SQLITE_OMIT_BLOB_LITERAL = c174_OmitFeatures.addChild("c241_SQLITE_OMIT_BLOB_LITERAL").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c248_SQLITE_OMIT_BTREECOUNT = c174_OmitFeatures.addChild("c248_SQLITE_OMIT_BTREECOUNT").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c255_SQLITE_OMIT_BUILTIN_TEST = c174_OmitFeatures.addChild("c255_SQLITE_OMIT_BUILTIN_TEST").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c262_SQLITE_OMIT_CAST = c174_OmitFeatures.addChild("c262_SQLITE_OMIT_CAST").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c269_SQLITE_OMIT_CHECK = c174_OmitFeatures.addChild("c269_SQLITE_OMIT_CHECK").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c276_SQLITE_OMIT_COMPILEOPTION_DIAGS = c174_OmitFeatures.addChild("c276_SQLITE_OMIT_COMPILEOPTION_DIAGS").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c283_SQLITE_OMIT_COMPLETE = c174_OmitFeatures.addChild("c283_SQLITE_OMIT_COMPLETE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c290_SQLITE_OMIT_COMPOUND_SELECT = c174_OmitFeatures.addChild("c290_SQLITE_OMIT_COMPOUND_SELECT").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c297_SQLITE_OMIT_CONFLICT_CLAUSE = c174_OmitFeatures.addChild("c297_SQLITE_OMIT_CONFLICT_CLAUSE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c303_SQLITE_OMIT_DATETIME_FUNCS = c174_OmitFeatures.addChild("c303_SQLITE_OMIT_DATETIME_FUNCS").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c310_SQLITE_OMIT_DECLTYPE = c174_OmitFeatures.addChild("c310_SQLITE_OMIT_DECLTYPE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c319_SQLITE_OMIT_DEPRECATED = c174_OmitFeatures.addChild("c319_SQLITE_OMIT_DEPRECATED").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c326_SQLITE_OMIT_EXPLAIN = c174_OmitFeatures.addChild("c326_SQLITE_OMIT_EXPLAIN").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c333_SQLITE_OMIT_FLAG_PRAGMAS = c174_OmitFeatures.addChild("c333_SQLITE_OMIT_FLAG_PRAGMAS").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c340_SQLITE_OMIT_FLOATING_POINT = c174_OmitFeatures.addChild("c340_SQLITE_OMIT_FLOATING_POINT").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c347_SQLITE_OMIT_FOREIGN_KEY = c174_OmitFeatures.addChild("c347_SQLITE_OMIT_FOREIGN_KEY").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c354_SQLITE_OMIT_GET_TABLE = c174_OmitFeatures.addChild("c354_SQLITE_OMIT_GET_TABLE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c361_SQLITE_OMIT_INCRBLOB = c174_OmitFeatures.addChild("c361_SQLITE_OMIT_INCRBLOB").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c368_SQLITE_OMIT_INTEGRITY_CHECK = c174_OmitFeatures.addChild("c368_SQLITE_OMIT_INTEGRITY_CHECK").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c375_SQLITE_OMIT_LIKE_OPTIMIZATION = c174_OmitFeatures.addChild("c375_SQLITE_OMIT_LIKE_OPTIMIZATION").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c382_SQLITE_OMIT_LOAD_EXTENSION = c174_OmitFeatures.addChild("c382_SQLITE_OMIT_LOAD_EXTENSION").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c389_SQLITE_OMIT_LOCALTIME = c174_OmitFeatures.addChild("c389_SQLITE_OMIT_LOCALTIME").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c396_SQLITE_OMIT_LOOKASIDE = c174_OmitFeatures.addChild("c396_SQLITE_OMIT_LOOKASIDE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c403_SQLITE_OMIT_MEMORYDB = c174_OmitFeatures.addChild("c403_SQLITE_OMIT_MEMORYDB").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c410_SQLITE_OMIT_OR_OPTIMIZATION = c174_OmitFeatures.addChild("c410_SQLITE_OMIT_OR_OPTIMIZATION").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c417_SQLITE_OMIT_PAGER_PRAGMAS = c174_OmitFeatures.addChild("c417_SQLITE_OMIT_PAGER_PRAGMAS").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c424_SQLITE_OMIT_PRAGMA = c174_OmitFeatures.addChild("c424_SQLITE_OMIT_PRAGMA").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c431_SQLITE_OMIT_PROGRESS_CALLBACK = c174_OmitFeatures.addChild("c431_SQLITE_OMIT_PROGRESS_CALLBACK").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c438_SQLITE_OMIT_QUICKBALANCE = c174_OmitFeatures.addChild("c438_SQLITE_OMIT_QUICKBALANCE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c445_SQLITE_OMIT_REINDEX = c174_OmitFeatures.addChild("c445_SQLITE_OMIT_REINDEX").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c452_SQLITE_OMIT_SCHEMA_PRAGMAS = c174_OmitFeatures.addChild("c452_SQLITE_OMIT_SCHEMA_PRAGMAS").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c459_SQLITE_OMIT_SCHEMA_VERSION_PRAGMAS = c174_OmitFeatures.addChild("c459_SQLITE_OMIT_SCHEMA_VERSION_PRAGMAS").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c466_SQLITE_OMIT_SHARED_CACHE = c174_OmitFeatures.addChild("c466_SQLITE_OMIT_SHARED_CACHE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c473_SQLITE_OMIT_SUBQUERY = c174_OmitFeatures.addChild("c473_SQLITE_OMIT_SUBQUERY").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c480_SQLITE_OMIT_TCL_VARIABLE = c174_OmitFeatures.addChild("c480_SQLITE_OMIT_TCL_VARIABLE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c486_SQLITE_OMIT_TEMPDB = c174_OmitFeatures.addChild("c486_SQLITE_OMIT_TEMPDB").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c492_SQLITE_OMIT_TRACE = c174_OmitFeatures.addChild("c492_SQLITE_OMIT_TRACE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c499_SQLITE_OMIT_TRIGGER = c174_OmitFeatures.addChild("c499_SQLITE_OMIT_TRIGGER").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c506_SQLITE_OMIT_TRUNCATE_OPTIMIZATION = c174_OmitFeatures.addChild("c506_SQLITE_OMIT_TRUNCATE_OPTIMIZATION").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c512_SQLITE_OMIT_UTF16 = c174_OmitFeatures.addChild("c512_SQLITE_OMIT_UTF16").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c519_SQLITE_OMIT_VACUUM = c174_OmitFeatures.addChild("c519_SQLITE_OMIT_VACUUM").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c526_SQLITE_OMIT_VIEW = c174_OmitFeatures.addChild("c526_SQLITE_OMIT_VIEW").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c533_SQLITE_OMIT_VIRTUALTABLE = c174_OmitFeatures.addChild("c533_SQLITE_OMIT_VIRTUALTABLE").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c540_SQLITE_OMIT_WAL = c174_OmitFeatures.addChild("c540_SQLITE_OMIT_WAL").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c547_SQLITE_OMIT_XFER_OPT = c174_OmitFeatures.addChild("c547_SQLITE_OMIT_XFER_OPT").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c554_SQLITE_DEBUG = c3_SQLite.addChild("c554_SQLITE_DEBUG").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c560_SQLITE_MEMDEBUG = c3_SQLite.addChild("c560_SQLITE_MEMDEBUG").withCard(0, 1).extending(c1_IMeasurable);
        AstConcreteClafer c566_simpleConfig = model.addChild("c566_simpleConfig").withCard(1, 1).extending(c3_SQLite);
        c2_footprint.refToUnique(IntType);
        c4_OperatingSystemCharacteristics.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(888)));
        c10_SQLITE_4_BYTE_ALIGNED_MALLOC.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c16_SQLITE_CASE_SENSITIVE_LIKE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c22_SQLITE_HAVE_ISNAN.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c28_SQLITE_SECURE_DELETE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c34_ChooseSQLITE_TEMP_STORE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c40_SQLITE_TEMP_STORE_EQ_0.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c46_SQLITE_TEMP_STORE_EQ_1.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c52_SQLITE_TEMP_STORE_EQ_2.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c58_SQLITE_TEMP_STORE_EQ_3.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c64_EnableFeatures.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c70_SQLITE_ENABLE_ATOMIC_WRITE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(3)));
        c76_SQLITE_ENABLE_COLUMN_METADATA.addConstraint(none(join(join(joinParent(joinParent($this())), c174_OmitFeatures), c310_SQLITE_OMIT_DECLTYPE)));
        c76_SQLITE_ENABLE_COLUMN_METADATA.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(2)));
        c84_SQLITE_ENABLE_FTS3.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(100)));
        c90_SQLITE_ENABLE_FTS3_PARENTHESIS.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c96_SQLITE_ENABLE_MEMORY_MANAGEMENT.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(1)));
        c102_ChooseMemSys.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(3)));
        c108_SQLITE_ENABLE_MEMSYS3.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(2)));
        c114_SQLITE_ENABLE_MEMSYS5.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c120_SQLITE_ENABLE_RTREE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(33)));
        c126_SQLITE_ENABLE_STAT2.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(4)));
        c132_SQLITE_ENABLE_UPDATE_DELETE_LIMIT.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(1)));
        c138_SQLITE_ENABLE_UNLOCK_NOTIFY.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(3)));
        c144_SQLITE_SOUNDEX.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(1)));
        c150_YYTRACKMAXSTACKDEPTH.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c156_DisableFeatures.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c162_SQLITE_DISABLE_LFS.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c168_SQLITE_DISABLE_DIRSYNC.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c174_OmitFeatures.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c180_SQLITE_OMIT_ALTERTABLE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-7)));
        c187_SQLITE_OMIT_ANALYZE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-5)));
        c194_SQLITE_OMIT_ATTACH.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-8)));
        c201_SQLITE_OMIT_AUTHORIZATION.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-5)));
        c208_SQLITE_OMIT_AUTOINCREMENT.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-3)));
        c215_SQLITE_OMIT_AUTOMATIC_INDEX.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-4)));
        c222_SQLITE_OMIT_AUTOINIT.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c228_SQLITE_OMIT_AUTOVACUUM.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-14)));
        c235_SQLITE_OMIT_BETWEEN_OPTIMIZATION.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c241_SQLITE_OMIT_BLOB_LITERAL.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-1)));
        c248_SQLITE_OMIT_BTREECOUNT.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-2)));
        c255_SQLITE_OMIT_BUILTIN_TEST.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-2)));
        c262_SQLITE_OMIT_CAST.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-1)));
        c269_SQLITE_OMIT_CHECK.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-1)));
        c276_SQLITE_OMIT_COMPILEOPTION_DIAGS.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-1)));
        c283_SQLITE_OMIT_COMPLETE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-888)));
        c290_SQLITE_OMIT_COMPOUND_SELECT.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-10)));
        c297_SQLITE_OMIT_CONFLICT_CLAUSE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c303_SQLITE_OMIT_DATETIME_FUNCS.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-10)));
        c310_SQLITE_OMIT_DECLTYPE.addConstraint(none(join(join(joinParent(joinParent($this())), c64_EnableFeatures), c76_SQLITE_ENABLE_COLUMN_METADATA)));
        c310_SQLITE_OMIT_DECLTYPE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-1)));
        c319_SQLITE_OMIT_DEPRECATED.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-1)));
        c326_SQLITE_OMIT_EXPLAIN.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-10)));
        c333_SQLITE_OMIT_FLAG_PRAGMAS.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-1)));
        c340_SQLITE_OMIT_FLOATING_POINT.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-18)));
        c347_SQLITE_OMIT_FOREIGN_KEY.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-15)));
        c354_SQLITE_OMIT_GET_TABLE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-3)));
        c361_SQLITE_OMIT_INCRBLOB.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-7)));
        c368_SQLITE_OMIT_INTEGRITY_CHECK.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-9)));
        c375_SQLITE_OMIT_LIKE_OPTIMIZATION.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-2)));
        c382_SQLITE_OMIT_LOAD_EXTENSION.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-3)));
        c389_SQLITE_OMIT_LOCALTIME.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-1)));
        c396_SQLITE_OMIT_LOOKASIDE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-1)));
        c403_SQLITE_OMIT_MEMORYDB.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-2)));
        c410_SQLITE_OMIT_OR_OPTIMIZATION.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-5)));
        c417_SQLITE_OMIT_PAGER_PRAGMAS.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-5)));
        c424_SQLITE_OMIT_PRAGMA.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-16)));
        c431_SQLITE_OMIT_PROGRESS_CALLBACK.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-1)));
        c438_SQLITE_OMIT_QUICKBALANCE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-1)));
        c445_SQLITE_OMIT_REINDEX.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-1)));
        c452_SQLITE_OMIT_SCHEMA_PRAGMAS.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-2)));
        c459_SQLITE_OMIT_SCHEMA_VERSION_PRAGMAS.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-1)));
        c466_SQLITE_OMIT_SHARED_CACHE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-10)));
        c473_SQLITE_OMIT_SUBQUERY.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-11)));
        c480_SQLITE_OMIT_TCL_VARIABLE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c486_SQLITE_OMIT_TEMPDB.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c492_SQLITE_OMIT_TRACE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-3)));
        c499_SQLITE_OMIT_TRIGGER.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-27)));
        c506_SQLITE_OMIT_TRUNCATE_OPTIMIZATION.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(0)));
        c512_SQLITE_OMIT_UTF16.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-8)));
        c519_SQLITE_OMIT_VACUUM.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-4)));
        c526_SQLITE_OMIT_VIEW.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-3)));
        c533_SQLITE_OMIT_VIRTUALTABLE.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-20)));
        c540_SQLITE_OMIT_WAL.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-30)));
        c547_SQLITE_OMIT_XFER_OPT.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(-3)));
        c554_SQLITE_DEBUG.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(9)));
        c560_SQLITE_MEMDEBUG.addConstraint(equal(joinRef(join($this(), c2_footprint)), constant(2)));

        ClaferOptimizer solver = ClaferCompiler.compile(model, Scope.defaultScope(200)
                .intLow(-10000).intHigh(10000), Objective.minimize(sum(global(c2_footprint))));
        assertTrue(solver.find());
        assertArrayEquals(new int[]{-299}, solver.optimalValues());
    }

    /**
     * <pre>
     * abstract Feature
     *     cost -> int
     *
     * A : Feature
     * B : Feature
     * </pre>
     */
    @Test(timeout = 60000)
    public void testSumFeatureCost() {
        AstModel model = newModel();

        AstAbstractClafer feature = model.addAbstract("Feature");
        AstConcreteClafer cost = feature.addChild("cost").refToUnique(IntType).withCard(Mandatory);
        AstConcreteClafer a = model.addChild("A").extending(feature).withCard(Mandatory);
        AstConcreteClafer b = model.addChild("B").extending(feature).withCard(Mandatory);

        ClaferOptimizer solver = ClaferCompiler.compile(model, Scope.defaultScope(200)
                .intLow(-8).intHigh(7), Objective.minimize(sum(global(cost))));
        assertTrue(solver.find());
        assertArrayEquals(new int[]{-16}, solver.optimalValues());
    }
}
