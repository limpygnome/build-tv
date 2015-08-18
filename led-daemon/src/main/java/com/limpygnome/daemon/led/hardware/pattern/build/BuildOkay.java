package com.limpygnome.daemon.led.hardware.pattern.build;

import com.limpygnome.daemon.led.hardware.Pattern;
import com.limpygnome.daemon.led.hardware.LedController;
import com.limpygnome.daemon.led.hardware.LedRenderThread;

/**
 * Created by limpygnome on 18/07/15.
 */
public class BuildOkay implements Pattern
{
    @Override
    public void render(LedRenderThread ledRenderThread, LedController ledController) throws InterruptedException
    {
        ledController.setStrip(0, 255, 0);
        ledController.render();
    }
}