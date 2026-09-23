import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe LRU cache. HashMap + doubly-linked list, one lock.
 * Version B (striped) would wrap N of these under a hash-to-shard router.
 */
public final class LRUCache<K, V> {

    private static final class Node<K, V> {
        final K key;
        V value;
        Node<K, V> prev, next;
        Node(K k, V v) { this.key = k; this.value = v; }
    }

    private final int capacity;
    private final Map<K, Node<K, V>> index = new HashMap<>();
    private final Node<K, V> head = new Node<>(null, null);
    private final Node<K, V> tail = new Node<>(null, null);
    private final ReentrantLock lock = new ReentrantLock();

    public LRUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }

    public V get(K key) {
        lock.lock();
        try {
            Node<K, V> n = index.get(key);
            if (n == null) return null;
            moveToFront(n);
            return n.value;
        } finally { lock.unlock(); }
    }

    public void put(K key, V value) {
        lock.lock();
        try {
            Node<K, V> n = index.get(key);
            if (n != null) { n.value = value; moveToFront(n); return; }
            n = new Node<>(key, value);
            index.put(key, n);
            addToFront(n);
            if (index.size() > capacity) {
                Node<K, V> lru = tail.prev;
                remove(lru);
                index.remove(lru.key);
            }
        } finally { lock.unlock(); }
    }

    public int size() {
        lock.lock();
        try { return index.size(); }
        finally { lock.unlock(); }
    }

    private void addToFront(Node<K, V> n) {
        n.prev = head; n.next = head.next;
        head.next.prev = n; head.next = n;
    }
    private void remove(Node<K, V> n) {
        n.prev.next = n.next; n.next.prev = n.prev;
        n.prev = n.next = null;
    }
    private void moveToFront(Node<K, V> n) { remove(n); addToFront(n); }
}
