package repli;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigReader {
    private final Properties props = new Properties();

    public ConfigReader(String filePath) {
        try {
            try (FileInputStream fis = new FileInputStream(filePath)) {
                this.props.load(fis);
            }

        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer config.properties: " + e.getMessage());
        }
    }

    public String get(String key) {
        return this.props.getProperty(key);
    }
}






