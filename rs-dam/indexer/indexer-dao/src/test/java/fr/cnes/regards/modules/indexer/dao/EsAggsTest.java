package fr.cnes.regards.modules.indexer.dao;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.cnes.regards.framework.gson.adapters.MultimapAdapter;
import fr.cnes.regards.framework.jsoniter.IIndexableJsoniterConfig;
import fr.cnes.regards.framework.multitenant.test.SingleRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.indexer.dao.builder.AggregationBuilderFacetTypeVisitor;
import fr.cnes.regards.modules.indexer.dao.deser.JsoniterDeserializeIIndexableStrategy;
import fr.cnes.regards.modules.indexer.dao.mapping.utils.AttrDescToJsonMapping;
import fr.cnes.regards.modules.indexer.domain.IDocFiles;
import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Test on complex aggs
 *
 * @author oroussel
 */
public class EsAggsTest {

    /**
     * 150 Mb file size to generate
     */
    public static final int BIG_FILE_SIZE_IN_BYTES = 1024 * 1024 * 150;

    /**
     * 5 Mb file size to generate
     */
    public static final int MEDIUM_FILE_SIZE_IN_BYTES = 1024 * 1024 * 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(EsAggsTest.class);

    /**
     * Class to test
     */
    private static IEsRepository repository;

    /**
     * JSON mapper
     */
    private static Gson gson;

    private static final String INDEX = "aggstest";

    private static final String TYPE = "DATA";

    /**
     * Before class setting up method
     *
     * @throws Exception exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
        Map<String, String> propMap = Maps.newHashMap();
        boolean repositoryOK = true;
        // we get the properties into target/test-classes because this is where maven will put the filtered file(with
        // real values and not placeholder)
        Stream<String> props = Files.lines(Paths.get("target/test-classes/test.properties"));
        props.filter(line -> !(line.startsWith("#") || line.trim().isEmpty())).forEach(line -> {
            String[] keyVal = line.split("=");
            propMap.put(keyVal[0], keyVal[1]);
        });
        try {
            gson = new GsonBuilder().registerTypeAdapter(Multimap.class, new MultimapAdapter()).create();
            repository = new EsRepository(gson,
                                          new JsoniterDeserializeIIndexableStrategy(new IIndexableJsoniterConfig()),
                                          new AggregationBuilderFacetTypeVisitor(10, 1),
                                          new AttrDescToJsonMapping(AttrDescToJsonMapping.RangeAliasStrategy.GTELTE),
                                          new SingleRuntimeTenantResolver("test"),
                                          Collections.emptyList(),
                                          propMap.get("regards.elasticsearch.host"),
                                          Integer.parseInt(propMap.get("regards.elasticsearch.http.port")),
                                          propMap.get("regards.elasticsearch.http.protocol"),
                                          null,
                                          null,
                                          0,
                                          15000,
                                          1200000);
        } catch (NoNodeAvailableException e) {
            LOGGER.error("NO NODE AVAILABLE");
            repositoryOK = false;
        }
        // Do not launch tests is Elasticsearch is not available
        Assume.assumeTrue(repositoryOK);
    }

    private static final String[] TAGS = new String[] { "RIRI", "FIFI", "LOULOU", "MICHOU", "JOJO" };

    private static final Random random = new Random();

    private static Set<String> randomTags() {
        Set<String> randomSet = new HashSet<>();
        int size = random.nextInt(TAGS.length) + 1;
        if (size == TAGS.length) {
            return Sets.newHashSet(TAGS);
        }
        for (int i = 0; i < size; i++) {
            while (!randomSet.add(TAGS[random.nextInt(TAGS.length)])) {
            }
        }
        return randomSet;
    }

    private void createDatas() throws IOException {
        if (repository.indexExists(INDEX)) {
            repository.deleteAll(INDEX);
        } else {
            repository.createIndex(INDEX);
        }

        Set<Data> datas = new HashSet<>();
        File rootDir = Paths.get("src/test/resources/testdir").toFile();
        for (File file : rootDir.listFiles()) {
            System.out.println(file.getName() + ", " + file.length());
            datas.add(buildData(file));
        }
        datas.add(buildDataFromGeneratedFile("jean.tar.gz", BIG_FILE_SIZE_IN_BYTES));
        datas.add(buildDataFromGeneratedFile("Pierre.mp4", MEDIUM_FILE_SIZE_IN_BYTES));
        repository.saveBulk(INDEX, datas);
    }

    @NotNull
    private static Data buildData(File file) {
        Data data = new Data();
        data.setDocId(file.getName());
        data.setTags(randomTags());
        data.getFiles().put(DataType.RAWDATA, new DataFile(file, DataType.RAWDATA));
        data.getFiles().put(DataType.QUICKLOOK_HD, new DataFile(file, DataType.QUICKLOOK_HD));
        return data;
    }

    private @NotNull Data buildDataFromGeneratedFile(String fileName, int fileSize) throws IOException {

        File file = new File("src/test/resources/testdir/" + fileName);
        file.getParentFile().mkdirs();

        try (RandomAccessFile f = new RandomAccessFile(file, "rw")) {
            f.setLength(fileSize);
            return buildData(file);
        }
    }

    @Test
    public void test() throws IOException {
        createDatas();
        DocFilesSummary summary = new DocFilesSummary();
        SimpleSearchKey<Data> searchKey = new SimpleSearchKey<>(TYPE, Data.class);
        searchKey.setSearchIndex(INDEX);
        repository.computeInternalDataFilesSummary(searchKey,
                                                   null,
                                                   "tags",
                                                   Optional.empty(),
                                                   summary,
                                                   "RAWDATA",
                                                   "QUICKLOOK_HD");
        System.out.println(summary);
        Assert.assertEquals(12, summary.getDocumentsCount());
        // 24 because 12 RAWDATA and 12 QUICKLOOKS
        Assert.assertEquals(24, summary.getFilesCount());
        Assert.assertEquals(327237376, summary.getFilesSize());
        Assert.assertTrue(summary.getSubSummariesMap().containsKey("FIFI"));
        Assert.assertTrue(summary.getSubSummariesMap().containsKey("RIRI"));
        Assert.assertTrue(summary.getSubSummariesMap().containsKey("LOULOU"));
        Assert.assertTrue(summary.getSubSummariesMap().containsKey("FIFI"));
        // assert aggregates details
        Assert.assertEquals(12, summary.getFileTypesSummaryMap().get("RAWDATA_ref").getFilesCount());
        Assert.assertEquals(0, summary.getFileTypesSummaryMap().get("RAWDATA_!ref").getFilesCount());
        Assert.assertEquals(0, summary.getFileTypesSummaryMap().get("QUICKLOOK_HD_ref").getFilesCount());
        Assert.assertEquals(12, summary.getFileTypesSummaryMap().get("QUICKLOOK_HD_!ref").getFilesCount());
    }

    private static class Feature {

        private final Multimap<DataType, fr.cnes.regards.modules.indexer.domain.DataFile> files = HashMultimap.create();

        public Multimap<DataType, fr.cnes.regards.modules.indexer.domain.DataFile> getFiles() {
            return files;
        }

    }

    private static class Data implements IIndexable, IDocFiles {

        private String docId;

        private final String type = TYPE;

        private Set<String> tags = new HashSet<>();

        private final Feature feature = new Feature();

        public Data() {
        }

        @SuppressWarnings("unused")
        public Data(String docId, Set<String> tags) {
            this.docId = docId;
            this.tags = tags;
        }

        @Override
        public String getDocId() {
            return docId;
        }

        @Override
        public String getType() {
            return type;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }

        @Override
        public String getLabel() {
            return docId;
        }

        @SuppressWarnings("unused")
        public Set<String> getTags() {
            return tags;
        }

        public void setTags(Set<String> tags) {
            this.tags = tags;
        }

        @Override
        public Multimap<DataType, fr.cnes.regards.modules.indexer.domain.DataFile> getFiles() {
            return feature.getFiles();
        }
    }

    private static class DataFile extends fr.cnes.regards.modules.indexer.domain.DataFile {

        @SuppressWarnings("unused")
        public DataFile() {
        }

        public DataFile(File file, DataType type) {
            this.setFilesize(file.length());
            switch (type) {
                case RAWDATA:
                    super.setUri(file.toURI().toString());
                    super.setReference(true);
                    break;
                case QUICKLOOK_HD:
                    super.setUri(new File(file.getParentFile(), file.getName() + "_QL_HD").toURI().toString());
                    super.setReference(false);
                    break;
                case QUICKLOOK_MD:
                    super.setUri(new File(file.getParentFile(), file.getName() + "_QL_MD").toURI().toString());
                    break;
                case QUICKLOOK_SD:
                    super.setUri(new File(file.getParentFile(), file.getName() + "_QL_SD").toURI().toString());
                    break;
                case DESCRIPTION:
                case AIP:
                case DOCUMENT:
                case OTHER:
                case THUMBNAIL:
                    throw new IllegalArgumentException("Unsupported data type : " + type);
            }
        }
    }
}
