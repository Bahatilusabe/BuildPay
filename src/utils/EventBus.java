package utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

public class EventBus {
    private static final Map<String, List<Runnable>> listeners = new ConcurrentHashMap<>();

    public static void subscribe(String event, Runnable r) {
        listeners.computeIfAbsent(event, k -> new CopyOnWriteArrayList<>()).add(r);
    }

    public static void unsubscribe(String event, Runnable r) {
        List<Runnable> list = listeners.get(event);
        if (list != null) list.remove(r);
    }

    public static void publish(String event) {
        List<Runnable> list = listeners.get(event);
        if (list == null) return;
        for (Runnable r : list) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }
}
