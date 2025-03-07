package fr.cnes.regards.modules.crawler.plugins;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursor;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.DataSourceException;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IDataSourcePlugin;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.model.domain.Model;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Data source from json file for tests
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(id = "TestDataSourcePlugin",
        version = "2.0-SNAPSHOT",
        description = "Allows data extraction from a json file for tests",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class TestDataSourcePlugin implements IDataSourcePlugin {

    public static final String MODEL = "model";

    @PluginParameter(label = MODEL, name = MODEL)
    private Model dataModel;

    @Autowired
    private Gson gson;

    @Override
    public int getRefreshRate() {
        return 0;
    }

    @Override
    public String getModelName() {
        return dataModel.getName();
    }

    @Override
    public List<DataObjectFeature> findAll(String tenant, CrawlingCursor cursor, OffsetDateTime from, OffsetDateTime to)
        throws DataSourceException {
        File file = Paths.get("src", "test", "resources", "validation", "json", "validationData.json").toFile();
        List<DataObjectFeature> content;
        TypeToken<List<DataObjectFeature>> typeToken = new TypeToken<>() {

        };
        try {
            content = gson.fromJson(new JsonReader(new FileReader(file)), typeToken.getType());
        } catch (FileNotFoundException e) {
            throw new DataSourceException("Could not find the file for validation data 1", e);
        }
        return content;
    }
}
