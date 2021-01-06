package io.github.mrp_v2.betterservergui.server.gui;

import com.google.common.collect.Sets;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Map;

public class ConsolePanel extends JPanel
{
    private static final Color suggestionsPanelBackground = new Color(0, 0, 0, 192);
    @Nullable private CommandSuggestionHelper commandSuggestionHelper = null;
    private boolean updateSuggestionsBeforeDrawing = false;

    public ConsolePanel(Font font)
    {
        super(new BorderLayout());
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                Sets.newHashSet(AWTKeyStroke.getAWTKeyStroke("shift RIGHT")));
        setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                Sets.newHashSet(AWTKeyStroke.getAWTKeyStroke("shift LEFT")));
        setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS,
                Sets.newHashSet(AWTKeyStroke.getAWTKeyStroke("shift UP")));
        setFocusTraversalKeys(KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS,
                Sets.newHashSet(AWTKeyStroke.getAWTKeyStroke("shift DOWN")));
        setFont(font);
    }

    public void initCommandSuggestionHelper(JTextField inputField, MinecraftServer server)
    {
        commandSuggestionHelper = new CommandSuggestionHelper(inputField, server, 0, 10);
        commandSuggestionHelper.init();
    }

    @Override public void paint(Graphics g)
    {
        super.paint(g);
        paintSuggestions(g);
    }

    private void paintSuggestions(Graphics g)
    {
        if (updateSuggestionsBeforeDrawing)
        {
            commandSuggestionHelper.updateSuggestions();
            updateSuggestionsBeforeDrawing = false;
        }
        if (commandSuggestionHelper.getSuggestions() == null)
        {
            return;
        }
        int xOffset = commandSuggestionHelper.getSuggestionsDisplayXOffset(g);
        g.setColor(suggestionsPanelBackground);
        int boxWidth = 0;
        Map<String, Boolean> suggestions = commandSuggestionHelper.getSuggestions().getSuggestions();
        for (String suggestion : suggestions.keySet())
        {
            boxWidth = Math.max(boxWidth,
                    (int) getFont().getStringBounds(suggestion, g.getFontMetrics().getFontRenderContext()).getWidth());
        }
        boxWidth += 4;
        int boxHeight = 4 + 16 * suggestions.size();
        g.fillRect(8 + xOffset, getHeight() - 48 - boxHeight, boxWidth + 4, boxHeight + 4);
        g.setColor(Color.WHITE);
        boxHeight -= 4;
        int i = 0;
        for (String suggestion : suggestions.keySet())
        {
            boolean selected = suggestions.get(suggestion);
            if (selected)
            {
                g.setColor(Color.YELLOW);
            }
            g.drawString(suggestion, 10 + xOffset, getHeight() - 50 - boxHeight + 16 * (i + 1));
            if (selected)
            {
                g.setColor(Color.WHITE);
            }
            i++;
        }
    }

    public EventListener getEventListener()
    {
        return new EventListener();
    }

    public LogEventListener getLogEventListener()
    {
        return new LogEventListener();
    }

    public class LogEventListener implements DocumentListener, AdjustmentListener
    {
        @Override public void insertUpdate(DocumentEvent e)
        {
            repaint();
        }

        @Override public void removeUpdate(DocumentEvent e)
        {
            repaint();
        }

        @Override public void changedUpdate(DocumentEvent e)
        {
        }

        @Override public void adjustmentValueChanged(AdjustmentEvent e)
        {
            repaint();
        }
    }

    public class EventListener implements KeyListener, CaretListener
    {
        @Override public void keyTyped(KeyEvent e)
        {
        }

        @Override public void keyPressed(KeyEvent e)
        {
            if (commandSuggestionHelper.onKeyPressed(e.getKeyCode()))
            {
                e.consume();
            }
            repaint();
        }

        @Override public void keyReleased(KeyEvent e)
        {
        }
        

        @Override public void caretUpdate(CaretEvent e)
        {
            if (commandSuggestionHelper.isApplyingSuggestion())
            {
                return;
            }
            commandSuggestionHelper.init();
            updateSuggestionsBeforeDrawing = true;
            repaint();
        }
    }
}
