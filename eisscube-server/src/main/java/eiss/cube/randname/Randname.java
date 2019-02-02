package eiss.cube.randname;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class Randname {

    private int pos;
    private Dictionary dictionary;

    @Inject
    public Randname(Dictionary dictionary) {
        this.dictionary = dictionary;
        pos = (int)System.currentTimeMillis();
    }

    public synchronized String next() {
        pos = Math.abs(pos + dictionary.getPrime()) % dictionary.getSize();
        return dictionary.getWord(pos);
    }

}
