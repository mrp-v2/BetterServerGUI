package io.github.mrp_v2.betterservergui.mixin;

import io.github.mrp_v2.betterservergui.server.gui.ConsolePanel;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.gui.MinecraftServerGui;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.swing.*;
import java.awt.*;

@Mixin(MinecraftServerGui.class) public abstract class MinecraftServerGuiMixin extends JComponent
{
    @Shadow @Final private static Font SERVER_GUI_FONT;
    private ConsolePanel consolePanel;
    @Shadow @Final private DedicatedServer server;

    @ModifyVariable(method = "getLogComponent", at = @At(value = "STORE", ordinal = 0))
    private JPanel onGetLogComponentAssignJPanel(JPanel original)
    {
        consolePanel = new ConsolePanel(SERVER_GUI_FONT);
        return consolePanel;
    }

    @Inject(method = "getLogComponent", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onGetLogComponent(CallbackInfoReturnable<JComponent> cir, JPanel jpanel, JTextArea jtextarea,
            JScrollPane jscrollpane, JTextField jtextfield)
    {
        ConsolePanel.EventListener eventListener = consolePanel.getEventListener();
        ConsolePanel.LogEventListener logEventListener = consolePanel.getLogEventListener();
        jtextfield.addKeyListener(eventListener);
        jtextfield.addCaretListener(eventListener);
        jtextarea.getDocument().addDocumentListener(logEventListener);
        jscrollpane.getVerticalScrollBar().addAdjustmentListener(logEventListener);
        jscrollpane.getHorizontalScrollBar().addAdjustmentListener(logEventListener);
        consolePanel.initCommandSuggestionHelper(jtextfield, server);
    }
}
