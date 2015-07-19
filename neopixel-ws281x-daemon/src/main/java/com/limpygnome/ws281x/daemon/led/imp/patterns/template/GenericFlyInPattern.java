package com.limpygnome.ws281x.daemon.led.imp.patterns.template;

import com.limpygnome.ws281x.daemon.led.imp.LedController;
import com.limpygnome.ws281x.daemon.led.imp.LedRenderThread;

/**
 * Created by limpygnome on 19/07/15.
 */
public class GenericFlyInPattern
{

    public static void  flyIn(LedRenderThread ledRenderThread, LedController ledController,
                              int red, int green, int blue, long frameDelay) throws InterruptedException
    {
        int leds = ledController.getLedsCount();
        int halfLeds = leds / 2;

        int currentIndex = 0;

        for (int i = 0; i < halfLeds && !ledRenderThread.isExit(); i++)
        {
            for (int j = 0; j < leds; j++)
            {
                if (j <= currentIndex || j >= (leds - currentIndex))
                {
                    ledController.setPixel(j, red, green, blue);
                }
                else
                {
                    ledController.setPixel(j, 0, 0, 0);
                }
            }


            ledController.render();
            Thread.sleep(frameDelay);
            currentIndex++;
        }
    }

}
