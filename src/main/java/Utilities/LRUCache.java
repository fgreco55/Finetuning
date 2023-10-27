package Utilities;

/*
 * Need an LRU for the prompt history
 */
import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    public LRUCache(int capacity) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

    /*
     * simple test
     */
    public static void main(String[] args) {
        int capacity = 3;
        LRUCache<Integer, String> cache = new LRUCache<>(capacity);

        cache.put(1, "One");
        cache.put(2, "Two");
        cache.put(3, "Three");
        System.out.println(cache);

        cache.put(4, "Four");
        System.out.println(cache);

        cache.put(8, "Seven");
        System.out.println(cache);

        cache.put(0, "frank");
        System.out.println(cache);

        //System.out.println(cache.get(2));
        //System.out.println(cache);
    }
}
