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

    @Inject(method = "tick", at = @At("HEAD")) private void tick(CallbackInfo ci)
    {
        for (int i = lastServerTickCount; i < server.getTickCounter(); i++)
        {
            tickTimes[i & 255] = (int) (server.tickTimeArray[i % 100] / 1000000);
        }
        lastServerTickCount = server.getTickCounter();
    }

    @Inject(method = "paint", at = @At("TAIL")) public void paint(Graphics p_paint_1_, CallbackInfo ci)
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
            if (tickTime == 0)
            {
                continue;
            }
            if (tickTime > 75)
            {
                tickTime = 75;
            }
            int tickTimeOutOf60 = tickTime * 60 / 75;
            int percentageOfMaxTickTime = tickTime * 100 / 50;
            if (percentageOfMaxTickTime > 100)
            {
                percentageOfMaxTickTime = 100;
            }
            int red = percentageOfMaxTickTime > 50 ? (255 * (percentageOfMaxTickTime - 50) / 50) : 0;
            int green = 255 - percentageOfMaxTickTime * 255 / 100;
            p_paint_1_.setColor(new Color(red, green, 0));
            p_paint_1_.fillRect(i, 148 + 60 - tickTimeOutOf60, 1, tickTimeOutOf60);
        }
    }
}