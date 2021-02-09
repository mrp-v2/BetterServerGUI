package io.github.mrp_v2.betterservergui.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.gui.StatsComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.swing.*;
import java.awt.*;

@Mixin(StatsComponent.class) public abstract class StatsComponentMixin extends JComponent
{
    private final int[] tickTimes = new int[256];
    private final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 0x100000);
    private final int halfMaxMemory = maxMemory / 2;
    private final int oneFourthMaxMemory = maxMemory / 4;
    private final int threeFourthsMaxMemory = halfMaxMemory + oneFourthMaxMemory;
    @Final @Shadow private MinecraftServer server;
    private int lastServerTickCount = 0;
    private final Timer tickTimeTickTimer = new Timer(50, (action) -> this.tickTimeTick());

    @Inject(method = "<init>", at = @At("RETURN")) private void onInitialization(CallbackInfo ci)
    {
        tickTimeTickTimer.start();
    }

    private void tickTimeTick()
    {
        for (int i = lastServerTickCount; i < server.getTickCounter(); i++)
        {
            tickTimes[i & 255] = (int) (server.tickTimeArray[i % 100] / 1000000);
        }
        lastServerTickCount = server.getTickCounter();
        repaint();
    }

    @Inject(method = "func_219053_a", at = @At("HEAD")) private void onStopTimers(CallbackInfo ci)
    {
        tickTimeTickTimer.stop();
    }

    @Inject(method = "tick", at = @At("HEAD")) private void onTick(CallbackInfo ci)
    {
        for (int i = lastServerTickCount; i < server.getTickCounter(); i++)
        {
            tickTimes[i & 255] = (int) (server.tickTimeArray[i % 100] / 1000000);
        }
        lastServerTickCount = server.getTickCounter();
    }

    @Inject(method = "paint", at = @At("TAIL")) public void onPaint(Graphics p_paint_1_, CallbackInfo ci)
    {
        p_paint_1_.setColor(Color.BLACK);
        p_paint_1_.drawString("- 0 mb", 257, 103);
        p_paint_1_.drawString("- " + oneFourthMaxMemory + " mb", 257, 78);
        p_paint_1_.drawString("- " + halfMaxMemory + " mb", 257, 53);
        p_paint_1_.drawString("- " + threeFourthsMaxMemory + " mb", 257, 28);
        p_paint_1_.drawString("Memory use over", 330, 53);
        p_paint_1_.drawString("128 seconds", 337, 66);
        p_paint_1_.drawString("ms/t over 256 ticks", 88, 225);
        p_paint_1_.drawString("- 75", 257, 151);
        p_paint_1_.drawString("- 50", 257, 171);
        p_paint_1_.drawString("- 25", 257, 191);
        p_paint_1_.drawString("- 0", 257, 211);
        for (int i = 0; i < 256; i++)
        {
            int tickTime = tickTimes[i + lastServerTickCount & 255];
            // it wouldn't show up on the graph, so just skip it
            if (tickTime == 0)
            {
                continue;
            }
            // clamp tick time to a max of 75
            if (tickTime > 75)
            {
                tickTime = 75;
            }
            // make the tick time a percentage of 50 ms
            tickTime *= 2;
            // adjust the tick time to be a value from 1-60
            int tickTimeOutOf60 = tickTime * 30 / 75;
            // clamp tick time to a max of 100%
            if (tickTime > 100)
            {
                tickTime = 100;
            }
            // red increases from 0 to 255 from tick time 0% - 50%
            int red = tickTime < 50 ? (255 * (tickTime) / 50) : 255;
            // green decreases linearly through the whole scale
            int green = 255 - tickTime * 255 / 100;
            p_paint_1_.setColor(new Color(red, green, 0));
            p_paint_1_.fillRect(i, 208 - tickTimeOutOf60, 1, tickTimeOutOf60);
        }
    }
}
