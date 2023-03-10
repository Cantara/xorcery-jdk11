package com.exoreaction.xorcery.configuration.model;

import com.exoreaction.xorcery.builders.WithContext;

public interface ServiceConfiguration
    extends WithContext<Configuration>
{
    default boolean isEnabled()
    {
        return context().getBoolean("enabled").orElse(false);
    }
}
