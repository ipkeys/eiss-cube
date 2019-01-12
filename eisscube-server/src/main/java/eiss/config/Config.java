package eiss.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    private final static String configDir = "config";
    private final static String keyFileName = "serverkey.txt";

    private String key;

    private Config() {
        Path file = Paths.get(HomeDir.getFolderPath(configDir).toString(), keyFileName);
        try {
            byte[] data = Files.readAllBytes(file);
            key = new String(data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            key = "changeme";
        }
    }

    private static class SingletonHolder {
        private static final Config INSTANCE = new Config();
    }

    public String getServerKey() {
        return key;
    }

    public static Config getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
