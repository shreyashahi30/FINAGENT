
package com.finagent;

import com.finagent.service.DatasetService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Paths;
import java.util.Arrays;

@SpringBootApplication
public class FinAgentApplication {

    public static void main(String[] args) throws Exception {

        if (Arrays.asList(args).contains("--export-sec-dataset")) {
            DatasetService datasetService = new DatasetService();

            if (!datasetService.isLoaded()) {
                throw new IllegalStateException(
                    "SEC dataset could not be loaded. Check data/sec-dataset."
                );
            }

            datasetService.exportProfiles(
                Paths.get("src/main/resources/sec/company-profiles.json")
            );

            System.out.println("SEC company profiles exported successfully.");
            System.out.println(datasetService.status());
            return;
        }

        SpringApplication.run(FinAgentApplication.class, args);
    }
}
