package com.tomtom.james.newagent;

public class GlobalValueStore {
    public static ValueStore valueStore = new ValueStore<Long>();

    private static ValueStore<Long> getValueStore() {
        try {
            return (ValueStore<Long>) ClassLoader.getSystemClassLoader()
                    .loadClass(GlobalValueStore.class.getName())
                    .getDeclaredField("valueStore")
                    .get(null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return valueStore;
    }

    public static void put(String key, long value) {
        getValueStore().put(Thread.currentThread().getId() + key, new Long(value));
    }

    public static long get(String key) {
        return getValueStore().get(Thread.currentThread().getId() + key);
    }

    public static long getAndRemove(String key){
        long value = get(key);
        remove(key);
        return value;
    }

    public static void remove(String key) {
        getValueStore().remove(Thread.currentThread().getId() + key);
    }
}
