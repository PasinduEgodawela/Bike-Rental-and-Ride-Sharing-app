package com.Webapp.BikeRentalSystem.Config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * The data files are found through the data.dir setting in application.properties.
 * A relative path such as src/main/resources/data only works when the app is started
 * from the project folder. If it is started from somewhere else the folder is not found
 * and empty files would be created in the wrong place, so this prints a clear warning.
 */
@Component
public class DataFolderCheck {

    private final String dataDir;

    public DataFolderCheck(@Value("${data.dir}") String dataDir) {
        this.dataDir = dataDir;
    }

    @PostConstruct
    public void check() {
        File folder = new File(dataDir);

        if (folder.isDirectory()) {
            System.out.println("Data folder: " + folder.getAbsolutePath());
        } else {
            System.out.println("WARNING: the data folder " + folder.getAbsolutePath() + " does not exist.");
            System.out.println("         Start the app from the project folder, or run it with"
                    + " --data.dir=<full path of the data folder>");
        }
    }
}
