package com.tomtom.james.newagent;

import java.lang.reflect.Field;

public class GlobalValueStore {
    public static ValueStore valueStore = null;

    static {
        try {
            valueStore = (ValueStore<Long>) ClassLoader.getSystemClassLoader()
                    .loadClass(GlobalValueStore.class.getName())
                    .getDeclaredField("valueStore")
                    .get(null);
            if (valueStore == null) {
                Field field;
                field = ClassLoader.getSystemClassLoader()
                        .loadClass(GlobalValueStore.class.getName())
                        .getDeclaredField("valueStore");
                field.setAccessible(true);
                field.set(null, new ValueStore<Long>());
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected static ValueStore<Long> getValueStore() {
            return valueStore;
    }

    public static void put(String key, long value) {
        getValueStore().put(Thread.currentThread().getId() + key, new Long(value));
    }

    public static long get(String key) {
        String key1 = Thread.currentThread().getId() + key;
        return getValueStore().get(
                key1
        );
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
