package com.limpygnome.daemon.system.service.stat;

import com.limpygnome.daemon.system.model.stat.Statistic;
import com.limpygnome.daemon.util.EnvironmentUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A stat service implementation to measure CPU usage.
 */
public class CpuStatService extends AbstractStatService
{
    private static final Logger LOG = LogManager.getLogger(CpuStatService.class);

    public static final String SERVICE_NAME = "stats_cpu";

    private static final String LABEL = "CPU";

    @Override
    public Statistic update()
    {
        final String[] BASH_COMMANDS = { "/bin/sh", "-c", "grep 'cpu ' /proc/stat | awk '{usage=($2+$4)*100/($2+$4+$5)} END {print usage}'" };

        Float value = EnvironmentUtil.execFloat(BASH_COMMANDS, DEFAULT_PROCESS_TIMEOUT, false);

        if (value == null)
        {
            LOG.warn("Failed to retrieve CPU usage, possibly unsupported");
            value = 0.0f;
        }

        // Round value
        value = (float) Math.round(value);

        final float min = 0.0f;
        final float max = 100.0f;

        return new Statistic(LABEL, min, max, value, "PERCENTAGE");
    }

}
