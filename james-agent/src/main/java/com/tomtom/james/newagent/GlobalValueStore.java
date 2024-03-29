package com.tomtom.james.newagent;

import java.lang.reflect.Field;

// FIXME clean sysout
public class GlobalValueStore {
    public static ValueStore valueStore = null;

    static {
        try {
            //System.out.println("------------------------------------------- GLOBAL VALUE STORE INITIALIZATION -------------------------------------------");
            valueStore = (ValueStore<Long>) ClassLoader.getSystemClassLoader()
                    .loadClass(GlobalValueStore.class.getName())
                    .getDeclaredField("valueStore")
                    .get(null);
            if (valueStore == null) {
                //System.out.println("------------------------------------------- GLOBAL VALUE STORE INITIALIZATION (SYSTEM) -------------------------------------------");
                Field field;
                field = ClassLoader.getSystemClassLoader()
                        .loadClass(GlobalValueStore.class.getName())
                        .getDeclaredField("valueStore");
                field.setAccessible(true);
                field.set(null, new ValueStore<Long>());
                //System.out.println(" --------------------------------------------- initial getValueStore : " + getValueStore());
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
        //System.out.println(" --------------------------------------------- GET ValueStore : " + valueStore);
        return valueStore;
    }

    public static void put(String key, long value) {
        //System.out.println(" --------------------------------------------- PUT VALUE TO ValueStore : " + valueStore);
        getValueStore().put(Thread.currentThread().getId() + key, value);
    }

    public static long get(String key) {
        //System.out.println(" --------------------------------------------- GET VALUE FROM ValueStore : " + valueStore);
        //System.out.println(" --------------------------------------------- getValueStore : " + getValueStore());
        String key1 = Thread.currentThread().getId() + key;
        Long aLong = getValueStore().get(key1);
        if (aLong == null) {
            aLong = -1L;
        }
        //System.out.println(" --------------------------------------------- ok aLong : " + aLong);
        return aLong;
    }

    public static long getAndRemove(String key) {
        //System.out.println(" --------------------------------------------- GET AND REMOVE VALUE FROM ValueStore : " + valueStore);
        long value = get(key);
        remove(key);
        return value;
    }

    public static void remove(String key) {
        //System.out.println(" --------------------------------------------- REMOVE VALUE FROM ValueStore : " + valueStore);
        getValueStore().remove(Thread.currentThread().getId() + key);
    }
}
