package cube.randname;

import cube.config.AppConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Data
public class Dictionary {

    private List<String> adjectives;
    private List<String> nouns;

    private int prime;
    private int size;

    @Inject
    public Dictionary(AppConfig cfg) {
        try {
            adjectives = Files.readAllLines(Paths.get(cfg.getRandNameConfig().getAdjectives()), UTF_8);
            nouns = Files.readAllLines(Paths.get(cfg.getRandNameConfig().getNouns()), UTF_8);
        } catch (IOException e) {
            throw new Error(e);
        }

        size = nouns.size() * adjectives.size();
        int primeCombo = 2;
        while (primeCombo <= size) {
            int nextPrime = primeCombo + 1;
            primeCombo *= nextPrime;
        }
        prime = primeCombo + 1;
    }

    String getWord(int i) {
        int a = i % adjectives.size();
        int n = i / adjectives.size();

        return adjectives.get(a) + "_" + nouns.get(n);
    }

}
