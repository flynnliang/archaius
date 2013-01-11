package com.netflix.config;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicPropertyUpdater {
    /**
     * Apply the ConfigurationUpdateResult to the configuration.<BR>
     * 
     * If the result is a full result from source, each property in the result is added/set in the configuration. Any
     * property that is in the configuration - but not in the result - is deleted if ignoreDeletesFromSource is false.<BR>
     * 
     * If the result is incremental, properties will be added and changed from the partial result in the configuration.
     * Deleted properties are deleted from configuration iff ignoreDeletesFromSource is false.
     */
    private static Logger logger = LoggerFactory.getLogger(DynamicPropertyUpdater.class);

    /**
     * Updates the properties in the config param given the contents of the result param.
     * 
     * TODO: this is a copy/paste from AbstractPollingScheduler which should be refactored to use this.
     * 
     * @param result
     *            either an incremental or full set of data
     * @param config
     *            underlying config map
     * @param ignoreDeletesFromSource
     *            if true, deletes will be skipped
     */
    public void updateProperties(final ConfigurationUpdateResult result, final Configuration config,
            final boolean ignoreDeletesFromSource) {
        //Preconditions.checkNotNull(config);

        if (result == null || !result.hasChanges()) {
            return;
        }

        logger.trace("incremental result? [{}]", result.isIncremental());
        logger.trace("ignored deletes from source? [{}]", ignoreDeletesFromSource);

        if (!result.isIncremental()) {
            Map<String, Object> props = result.getComplete();
            if (props == null) {
                return;
            }
            for (Entry<String, Object> entry : props.entrySet()) {
                addOrChangeProperty(entry.getKey(), entry.getValue(), config);
            }
            Set<String> existingKeys = new HashSet<String>();
            for (Iterator<String> i = config.getKeys(); i.hasNext();) {
                existingKeys.add(i.next());
            }
            if (!ignoreDeletesFromSource) {
                for (String key : existingKeys) {
                    if (!props.containsKey(key)) {
                        deleteProperty(key, config);
                    }
                }
            }
        } else {
            Map<String, Object> props = result.getAdded();
            if (props != null) {
                for (Entry<String, Object> entry : props.entrySet()) {
                    addOrChangeProperty(entry.getKey(), entry.getValue(), config);
                }
            }
            props = result.getChanged();
            if (props != null) {
                for (Entry<String, Object> entry : props.entrySet()) {
                    addOrChangeProperty(entry.getKey(), entry.getValue(), config);
                }
            }
            if (!ignoreDeletesFromSource) {
                props = result.getDeleted();
                if (props != null) {
                    for (String name : props.keySet()) {
                        deleteProperty(name, config);
                    }
                }
            }
        }
    }

    /**
     * Add or update the property in the underlying config depending on if it exists
     * 
     * TODO: this is a copy/paste from AbstractPollingScheduler which should be refactored to use this.
     * 
     * @param name
     * @param newValue
     * @param config
     */
    private void addOrChangeProperty(final String name, final Object newValue, final Configuration config) {
        if (!config.containsKey(name)) {
            logger.trace("adding property key [{}], value [{}]", name, newValue);

            config.addProperty(name, newValue);
        } else {
            Object oldValue = config.getProperty(name);
            if (newValue != null) {
                if (!newValue.equals(oldValue)) {
                    logger.trace("updating property key [{}], value [{}]", name, newValue);

                    config.setProperty(name, newValue);
                }
            } else if (oldValue != null) {
                logger.trace("nulling out property key [{}]", name);

                config.setProperty(name, null);
            }
        }
    }

    /**
     * Delete a property in the underlying config
     * 
     * TODO: this is a copy/paste from AbstractPollingScheduler which should be refactored to use this.
     * 
     * @param key
     * @param config
     */
    private void deleteProperty(final String key, final Configuration config) {
        if (config.containsKey(key)) {
            logger.trace("deleting property key [" + key + "]");

            config.clearProperty(key);
        }
    }
}