package Utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class History {
    private LRUCache<Integer, String> cache;
    private int counter = 0;        // number of prompt/completion entries
    private int maxcount = 10;      // only keep 10 lines of history as default

    public History(LRUCache<Integer, String> lc) {
        this.cache = lc;
    }

    public History(int init_size) {          // initial size (
        cache = new LRUCache<>(init_size);
    }

    public History(int init_size, int max) {
        this(init_size);         // initial capacity
        this.maxcount = max;    // max lines
    }

    public History() {
        cache = new LRUCache<>(1);      // default size of the LRU cache...
    }

    public LRUCache getLRUCache() {
        return cache;
    }

    public int size() {
        return cache.size();
    }

    public Set<Integer> getKeys() {         // to get elements in ordered set
        return cache.keySet();
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

    public List<String> toList() {
        List<String> strlist = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : cache.entrySet()) {
            strlist.add(entry.getValue());
        }
        return strlist;
    }

    public static void main(String[] argv) {
    	History h = new History();

    	h.add("miles");
        System.out.println("SIZE: " + h.size());

    	h.add("polly");
        System.out.println("SIZE: " + h.size());

    	h.add("festus");
        System.out.println("SIZE: " + h.size());

    	h.add("bibi");
        System.out.println("SIZE: " + h.size());

    	h.add("polly");
        System.out.println("SIZE: " + h.size());
        
    	h.toList().forEach(System.out::println);
    }
}