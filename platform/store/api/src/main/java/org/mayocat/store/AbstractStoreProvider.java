package org.mayocat.store;

import java.io.Serializable;
import java.lang.reflect.Type;

import javax.inject.Inject;

import org.mayocat.configuration.DataSourceConfiguration;
import org.mayocat.model.Entity;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

public abstract class AbstractStoreProvider<T extends Store<? extends Entity, ? extends Serializable>>
{
    @Inject
    private DataSourceConfiguration configuration;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    protected abstract Type getType();

    public T get()
    {
        try {
            return this.componentManager.getInstance(this.getType(), this.configuration.getName());
        } catch (ComponentLookupException e) {
            try {
                // Configured store failed. We warn in the console and as a fall-back plan try to get the default store

                this.logger.warn("Failed to lookup store for type {} and name {}, trying default store instead", this
                        .getType(), this.configuration.getName());
                return this.componentManager.getInstance(this.getType());
            } catch (ComponentLookupException ex) {
                // Default store failed to. Nothing more we can do.

                this.logger.error("Failed to lookup default fallback store for type {}", this.getType());
                throw new RuntimeException("No store for type [" + this.getType() + "] could be loaded.", ex);
            }
        }
    }
}