package com.limpygnome.daemon.led.service;

import com.limpygnome.daemon.api.Controller;
import com.limpygnome.daemon.api.Service;
import com.limpygnome.daemon.led.hardware.controller.LedController;
import com.limpygnome.daemon.led.model.LedSource;
import com.limpygnome.daemon.util.EnvironmentUtil;
import com.limpygnome.daemon.led.hardware.pattern.build.*;
import com.limpygnome.daemon.led.hardware.pattern.daemon.*;
import com.limpygnome.daemon.led.hardware.pattern.Pattern;
import com.limpygnome.daemon.led.hardware.controller.Ws281xLedController;
import com.limpygnome.daemon.led.hardware.LedRenderThread;
import com.limpygnome.daemon.led.hardware.pattern.team.Standup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * A service to control an LED strip.
 */
public class LedService implements Service
{
    private static final Logger LOG = LogManager.getLogger(LedService.class);

    public static final String SERVICE_NAME = "leds";

    /**
     * The name used by this daemon when internally setting an LED source.
     */
    public static final String INTERNAL_LED_SOURCE = "led-daemon";

    /**
     * The priority used by this daemon when internally setting an LED source.
     */
    public static final long INTERNAL_LED_PRIORITY = 0;

    private HashMap<String, Pattern> patterns;
    private LedController ledController;
    private LedRenderThread ledRenderThread;

    private HashMap<String, LedSource> ledSources;
    private LedSource currentLedSource;

    public LedService()
    {
        this.patterns = new HashMap<>();
        this.ledRenderThread = null;
        this.ledController = null;
        this.currentLedSource = null;

        // Setup two maps, one with sorting by value
        this.ledSources = new HashMap<>();
    }

    @Override
    public synchronized void start(Controller controller)
    {
        // Start LED controller
        if (EnvironmentUtil.isDevEnvironment())
        {
            LOG.warn("Dev environment detected, stubbing LED controller...");
            this.ledController = null;
        }
        else
        {
            LOG.info("Setting up Ws281x LED controller...");

            this.ledController = new Ws281xLedController(
                    controller.getSettings().getInt("led-controller/leds"),
                    controller.getSettings().getInt("led-controller/pin"),
                    controller.getSettings().getInt("led-controller/freq"),
                    controller.getSettings().getInt("led-controller/dma"),
                    controller.getSettings().getInt("led-controller/brightness"),
                    controller.getSettings().getInt("led-controller/pwm_channel"),
                    controller.getSettings().getBoolean("led-controller/invert")
            );
        }

        // Register all the patterns!
        // TODO: use annotations for magical scanning
        addPattern(new BuildUnknown());
        addPattern(new BuildOkay());
        addPattern(new BuildProgress());
        addPattern(new BuildUnstable());
        addPattern(new BuildFailure());
        addPattern(new JenkinsUnavailable());

        addPattern(new Shutdown());
        addPattern(new Startup());
        addPattern(new Test());
        addPattern(new Rainbow());

        addPattern(new Standup());

        LOG.info("{} LED patterns loaded", patterns.size());

        // Set startup pattern, whilst another service changes it
        setLedSource(INTERNAL_LED_SOURCE, "startup", INTERNAL_LED_PRIORITY);
    }

    private synchronized void addPattern(Pattern pattern)
    {
        patterns.put(pattern.getName(), pattern);
    }

    @Override
    public synchronized void stop(Controller controller)
    {
        // Change pattern to shutdown
        setLedSource(INTERNAL_LED_SOURCE, "shutdown", INTERNAL_LED_PRIORITY);

        // Stop render thread
        if (ledRenderThread != null)
        {
            ledRenderThread.kill();
            ledRenderThread = null;
        }

        // Wipe patterns
        patterns.clear();

        // Stop LED controller
        if (ledController != null)
        {
            ledController.dispose();
            ledController = null;
        }
    }

    public synchronized void setLedSource(String source, String patternName, long priority)
    {
        // Check params are valid
        if (source == null || source.length() == 0)
        {
            throw new IllegalArgumentException("Invalid source argument");
        }
        else if (patternName == null || patternName.length() == 0)
        {
            throw new IllegalArgumentException("Invalid pattern name");
        }

        // Locate pattern
        Pattern pattern = patterns.get(patternName);

        if (pattern == null)
        {
            LOG.warn("LED pattern missing - pattern: {}", patternName);
        }
        else
        {
            // Construct instance of LED pattern source
            LedSource ledSource = new LedSource(source, pattern, priority);

            // Add/update source
            ledSources.put(ledSource.getSource(), ledSource);

            LOG.debug("LED source updated - source: {}", ledSource);

            // Trigger check of LED source
            checkLedSource();
        }
    }

    public synchronized void removeLedSource(String source)
    {
        LedSource ledSource = ledSources.remove(source);

        if (ledSource != null)
        {
            LOG.info("Removed LED source - source: {}", source);

            // Check which source is now highest...
            checkLedSource();
        }
        else
        {
            LOG.info("Unable to remove non-existent LED source - source: {}", source);
        }
    }

    public synchronized LedSource getCurrentLedSource()
    {
        return currentLedSource;
    }

    public synchronized Map<String, LedSource> getLedSources()
    {
        return new HashMap<>(ledSources);
    }

    private synchronized void checkLedSource()
    {
        // Fetch highest item
        LedSource ledSourceHighest = fetchHighestLedSource();

        if (ledSourceHighest != null && (currentLedSource == null || currentLedSource != ledSourceHighest))
        {
            // Check if to switch the current pattern being rendered
            if  (currentLedSource == null || ledSourceHighest.getPattern() != currentLedSource.getPattern())
            {
                // Kill current thread
                if (ledRenderThread != null)
                {
                    ledRenderThread.kill();
                }

                // Start new thread
                ledRenderThread = new LedRenderThread(this, ledSourceHighest.getPattern());
                ledRenderThread.start();

                LOG.debug("LED pattern changed - source: {}", ledSourceHighest);
            }
            else
            {
                LOG.debug("LED pattern not updated, no difference - source: {}", ledSourceHighest);
            }

            currentLedSource = ledSourceHighest;
            LOG.debug("LED current pattern source updated - source: {}", ledSourceHighest);
        }
        else
        {
            LOG.debug("LED pattern not updated, no source or source not changed - source: {}", ledSourceHighest);
        }
    }

    private synchronized LedSource fetchHighestLedSource()
    {
        LedSource ledSourceHighest = null;
        LedSource ledSource;

        for (Map.Entry<String, LedSource> kv : ledSources.entrySet())
        {
            ledSource = kv.getValue();

            if (ledSourceHighest == null || ledSource.getPriority() > ledSourceHighest.getPriority())
            {
                ledSourceHighest = ledSource;
            }
        }

        return ledSourceHighest;
    }

    public synchronized LedController getLedController()
    {
        return ledController;
    }

}
