package com.limpygnome.ws281x.daemon.led.imp.patterns.build;

import com.limpygnome.ws281x.daemon.led.api.Pattern;
import com.limpygnome.ws281x.daemon.led.imp.LedController;
import com.limpygnome.ws281x.daemon.led.imp.LedRenderThread;
import com.limpygnome.ws281x.daemon.led.imp.patterns.template.GenericKnightRiderPattern;

/**
 * Created by limpygnome on 19/07/15.
 */
public class BuildFailure implements Pattern
{
    @Override
    public void render(LedRenderThread ledRenderThread, LedController ledController) throws InterruptedException
    {
        GenericKnightRiderPattern.renderKnightRiderEffect(ledRenderThread, ledController, 40, 255, 0, 0);
    }
}
