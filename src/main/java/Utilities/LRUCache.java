package Utilities;

/*
 * Need an LRU for the prompt history
 */
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;         //
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

        System.err.println("***************SIZE: " + cache.size());

        cache.put(1, "One");
        cache.put(2, "Two");
        cache.put(3, "Three");
        System.err.println("***************SIZE: " + cache.size());

        cache.put(4, "Four");
        System.err.println("***************SIZE: " + cache.size());

        cache.put(8, "Seven");
        System.err.println("***************SIZE: " + cache.size());

        cache.put(0, "frank");
        System.err.println("***************SIZE: " + cache.size());
    }
}
