package Utilities;

import java.util.Map;

public class PromptHistory {
    private LRUCache<Integer, String> cache;
    private int counter = 0;        // number of prompt/completion entries
    private int maxcount = 10;      // only keep 10 lines of history

    public PromptHistory(LRUCache<Integer, String> lc) {
        this.cache = lc;
    }
    public PromptHistory(int capacity) {
        cache = new LRUCache<>(capacity);
    }
    public PromptHistory(int capacity, int max) {
        this(capacity);
        this.maxcount = max;
    }

    public PromptHistory() {
        cache = new LRUCache<>(3);      // default size of the LRU cache...
    }

    public void add(String s) {
        if (counter++ > maxcount) {
            counter = 0;
            //System.err.println("-=-=-=-=-=-> HISTORY COUNTER CLEARED... HISTORY STILL INTACT");
        }

        cache.put(counter, s);      // Add new one... push oldest one out
    }

    public String toString() {
        String big = "";
        for (Map.Entry<Integer, String> entry : cache.entrySet()) {
            big += entry.getValue() + " ";
        }
        return big;
    }
}
