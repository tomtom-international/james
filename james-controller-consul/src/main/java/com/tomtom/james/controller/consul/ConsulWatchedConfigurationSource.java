package com.tomtom.james.controller.consul;

import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.KeyValueClient;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.MoreExecutors;

import com.netflix.config.WatchedConfigurationSource;
import com.netflix.config.WatchedUpdateListener;
import com.netflix.config.WatchedUpdateResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.BaseEncoding.base64;

/**
 * Implements WatchedConfigurationSource over a consul key-value store
 */
public class ConsulWatchedConfigurationSource extends AbstractExecutionThreadService implements WatchedConfigurationSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulWatchedConfigurationSource.class);

    private final String rootPath;
    private final KeyValueClient client;
    private final long watchIntervalSeconds;

    private final AtomicReference<ImmutableMap<String, Object>> lastState = new AtomicReference<>(null);
    private final AtomicLong latestIndex = new AtomicLong(0);
    private final String aclToken;

    private List<WatchedUpdateListener> listeners = new CopyOnWriteArrayList<>();


    private Response<List<GetValue>> getRaw(QueryParams params) {
        return client.getKVValues(rootPath, aclToken, params);
    }


    private Response<List<GetValue>> updateIndex(Response<List<GetValue>> response) {
        if (response != null) {
            this.latestIndex.set(response.getConsulIndex());
        }
        return response;
    }

    public ConsulWatchedConfigurationSource(String rootPath, KeyValueClient client) {
        this(rootPath, client, 10, TimeUnit.SECONDS, null);
    }

    public ConsulWatchedConfigurationSource(String rootPath, KeyValueClient client, long watchInterval, TimeUnit watchIntervalUnit, String aclToken) {
        this.rootPath = checkNotNull(rootPath);
        this.client = checkNotNull(client);
        this.watchIntervalSeconds = watchIntervalUnit.toSeconds(watchInterval);
        this.aclToken = aclToken;
    }

    private WatchedUpdateResult incrementalResult(
            final ImmutableMap<String, Object> newState,
            final ImmutableMap<String, Object> previousState) {

        final Map<String, Object> added = Maps.newHashMap();
        final Map<String, Object> removed = Maps.newHashMap();
        final Map<String, Object> changed = Maps.newHashMap();

        // added
        addAllKeys(
                Sets.difference(newState.keySet(), previousState.keySet()),
                newState, added

        );

        // removed
        addAllKeys(
                Sets.difference(previousState.keySet(), newState.keySet()),
                previousState, removed

        );

        // changed
        addFilteredKeys(
                Sets.intersection(previousState.keySet(), newState.keySet()),
                newState, changed,
                key -> !previousState.get(key).equals(newState.get(key))
                       );
        return WatchedUpdateResult.createIncremental(added, changed, removed);
    }

    private void addAllKeys(Set<String> keys, ImmutableMap<String, Object> source, Map<String, Object> dest) {
        addFilteredKeys(keys, source, dest, input -> true);
    }

    private void addFilteredKeys(Set<String> keys, ImmutableMap<String, Object> source, Map<String, Object> dest, Predicate<String> filter) {

        for (String key: keys) {
            if (filter.apply(key)) {
                dest.put(key, source.get(key));
            }
        }

    }

    protected void fireEvent(WatchedUpdateResult result) {
        for (WatchedUpdateListener l : listeners) {
            try {
                l.updateConfiguration(result);
            } catch (Throwable ex) {
                LOGGER.error("Error invoking WatchedUpdateListener", ex);
            }
        }
    }

    @Override
    public void addUpdateListener(WatchedUpdateListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    @Override
    public void removeUpdateListener(WatchedUpdateListener l) {
        if (l != null) {
            listeners.remove(l);
        }
    }

    @Override
    public Map<String, Object> getCurrentData() throws Exception {
        return lastState.get();
    }


    @VisibleForTesting
    protected long getLatestIndex() {
        return latestIndex.get();
    }

    private ImmutableMap<String, Object> convertToMap(Response<List<GetValue>> kv) {
        if (kv == null || kv.getValue() == null) {
            return ImmutableMap.of();
        }
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        for(GetValue gv : kv.getValue()) {
            Object value = valFunc(gv);

            // do not store "folders"
            if(value != null) {
                builder.put(keyFunc(gv), value);
            }
        }
        return builder.build();
    }

    private String valFunc(GetValue getValue) {
        String value = getValue.getValue();
        return value != null ? new String(base64().decode(value)) : null;
    }

    private String keyFunc(GetValue getValue) {
        return getValue.getKey().substring(rootPath.length() + 1);
    }

    @Override
    protected void run() throws Exception {
        while (isRunning()) {
            runOnce();
        }
    }

    public void runOnce() throws InterruptedException {
        try {
            Response<List<GetValue>> kvals = updateIndex(getRaw(watchParams()));
            ImmutableMap<String, Object> full = convertToMap(kvals);
            final WatchedUpdateResult result;
            if (lastState.get() == null) {
                result = WatchedUpdateResult.createFull(full);
            } else {
                result = incrementalResult(full, lastState.get());
            }
            lastState.set(full);
            fireEvent(result);
        } catch (Exception e) {
            LOGGER.error("Error watching path, waiting to retry", e);
            Thread.sleep(1000);
        }
    }

    private QueryParams watchParams() {
        return new QueryParams(watchIntervalSeconds, latestIndex.get());
    }

    @Override
    protected Executor executor() {
        return command -> {
            Thread thread = MoreExecutors.platformThreadFactory().newThread(command);
            thread.setDaemon(true);
            thread.setName(serviceName());
            thread.start();
        };
    }
}
