package eiss.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class return a {@link Path} related to EISS_HOME enviroment variable
 */
public final class HomeDir {

    private static final String EISS_HOME = "/eiss";

    private HomeDir() { }

    /**
     * Create EISS_HOME if not exists and return a {@link Path}
     * @return the resulting path
     */
    public static Path getHomePath() {
        Path rc;

        String home_env = System.getenv("EISS_HOME");
        if (home_env == null || home_env.isEmpty()) {
            rc = Paths.get(EISS_HOME);
        } else {
            rc = Paths.get(home_env);
        }

        try {
            rc = Files.createDirectories(rc);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rc;
    }

    /**
     * Create a new directory in EISS_HOME if not exists and return a {@link Path}
     * @param folder - name of directory
     * @return the resulting path
     */
    public static Path getFolderPath(String folder) {
        Path homeDir = getHomePath();
        Path rc = homeDir.resolve(folder);

        try {
            rc = Files.createDirectories(rc);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rc;
    }

}
