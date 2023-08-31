package io.github.mrp_v2.betterservergui.server.gui;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandSuggestionHelper
{
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");
    private final int minAmountRendered;
    private final int maxAmountRendered;
    private final JTextField inputField;
    public static final int TAB_KEY_CODE = AWTKeyStroke.getAWTKeyStroke("TAB").getKeyCode();
    public static final int UP_KEY_CODE = AWTKeyStroke.getAWTKeyStroke("UP").getKeyCode();
    public static final int DOWN_KEY_CODE = AWTKeyStroke.getAWTKeyStroke("DOWN").getKeyCode();
    public static final int ESCAPE_KEY_CODE = AWTKeyStroke.getAWTKeyStroke("ESCAPE").getKeyCode();
    private final MinecraftServer server;
    private CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestionsFuture;
    @Nullable private CommandSuggestionHelper.Suggestions suggestions;
    private boolean isApplyingSuggestion;
    @Nullable private ParseResults<CommandSource> parseResults;

    public boolean isApplyingSuggestion()
    {
        return isApplyingSuggestion;
    }

    public CommandSuggestionHelper(JTextField inputField, MinecraftServer server,
            int minAmountRendered, int maxAmountRendered)
    {
        this.inputField = inputField;
        this.server = server;
        this.minAmountRendered = minAmountRendered;
        this.maxAmountRendered = maxAmountRendered;
    }

    public boolean onKeyPressed(int keyCode)
    {
        if (suggestions != null && suggestions.onKeyPressed(keyCode))
        {
            return true;
        } else if (keyCode == TAB_KEY_CODE)
        {
            updateSuggestions();
            return true;
        } else
        {
            return false;
        }
    }

    public void updateSuggestions()
    {
        if (suggestionsFuture != null && suggestionsFuture.isDone())
        {
            com.mojang.brigadier.suggestion.Suggestions suggestions = suggestionsFuture.join();
            if (!suggestions.isEmpty())
            {
                this.suggestions = new CommandSuggestionHelper.Suggestions(getSuggestions(suggestions));
            }
        }
    }

    private List<Suggestion> getSuggestions(com.mojang.brigadier.suggestion.Suggestions suggestions)
    {
        String commandBeforeCursor = inputField.getText().substring(0, inputField.getCaretPosition());
        int lastWordIndex = getLastWhitespace(commandBeforeCursor);
        String lastWord = commandBeforeCursor.substring(lastWordIndex).toLowerCase(Locale.ROOT);
        List<Suggestion> vanillaSuggestions = Lists.newArrayList();
        List<Suggestion> moddedSuggestions = Lists.newArrayList();
        for (Suggestion suggestion : suggestions.getList())
        {
            if (!suggestion.getText().startsWith(lastWord) && !suggestion.getText().startsWith("minecraft:" + lastWord))
            {
                moddedSuggestions.add(suggestion);
            } else
            {
                vanillaSuggestions.add(suggestion);
            }
        }
        vanillaSuggestions.addAll(moddedSuggestions);
        return vanillaSuggestions;
    }

    public int getSuggestionsDisplayXOffset(Graphics g)
    {
        String inputText = inputField.getText();
        int lastWhitespaceIndex = inputText.lastIndexOf(" ");
        if (lastWhitespaceIndex == -1)
        {
            return 0;
        }
        Font font = inputField.getFont();
        return (int) font.getStringBounds(inputText.substring(0, lastWhitespaceIndex + 1),
                g.getFontMetrics(font).getFontRenderContext()).getWidth();
    }

    private static int getLastWhitespace(String text)
    {
        if (Strings.isNullOrEmpty(text))
        {
            return 0;
        } else
        {
            int i = 0;
            for (Matcher matcher = WHITESPACE_PATTERN.matcher(text); matcher.find(); i = matcher.end())
            {
            }
            return i;
        }
    }

    @Nullable public Suggestions getSuggestions()
    {
        return suggestions;
    }

    public void init()
    {
        String command = inputField.getText();
        if (parseResults != null && !parseResults.getReader().getString().equals(command))
        {
            parseResults = null;
        }
        if (!isApplyingSuggestion)
        {
            suggestions = null;
        }
        StringReader stringReader = new StringReader(command);
        if (stringReader.canRead() && stringReader.peek() == '/')
        {
            stringReader.skip();
        }
        int caretPosition = inputField.getCaretPosition();
        CommandDispatcher<CommandSource> commandDispatcher = server.getCommands().getDispatcher();
        if (parseResults == null)
        {
            parseResults = commandDispatcher.parse(stringReader, server.createCommandSourceStack());
        }
        if (caretPosition >= stringReader.getCursor() && (suggestions == null || !isApplyingSuggestion))
        {
            suggestionsFuture = commandDispatcher.getCompletionSuggestions(parseResults, caretPosition);
            suggestionsFuture.thenRun(() ->
            {
                if (suggestionsFuture.isDone())
                {
                    recompileSuggestions();
                }
            });
        }
    }

    private void recompileSuggestions()
    {
        if (inputField.getCaretPosition() == inputField.getText().length())
        {
            if (suggestionsFuture.join().isEmpty() && !parseResults.getExceptions().isEmpty())
            {
                int i = 0;
                for (Map.Entry<CommandNode<CommandSource>, CommandSyntaxException> entry : parseResults.getExceptions()
                        .entrySet())
                {
                    CommandSyntaxException commandsyntaxexception = entry.getValue();
                    if (commandsyntaxexception.getType() ==
                            CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect())
                    {
                        ++i;
                    }
                }
            }
        }
        suggestions = null;
        if (!inputField.getText().equals(""))
        {
            updateSuggestions();
        }
    }

    @OnlyIn(Dist.DEDICATED_SERVER) public class Suggestions
    {
        private final String originalInputText;
        private final List<Suggestion> suggestions;
        private int lowestDisplayedSuggestionIndex;
        private int selectedIndex;
        private boolean changeSelectionOnNextTabInput;

        private Suggestions(List<Suggestion> suggestions)
        {
            originalInputText = inputField.getText();
            this.suggestions = suggestions;
            selectSuggestion(0);
        }

        public Map<String, Boolean> getSuggestions()
        {
            int suggestionsDisplayCount = Math.min(suggestions.size(), maxAmountRendered);
            Map<String, Boolean> suggestionsMap = new LinkedHashMap<>(suggestionsDisplayCount);
            for (int i = 0; i < suggestionsDisplayCount; i++)
            {
                int index = i + lowestDisplayedSuggestionIndex;
                suggestionsMap.put(suggestions.get(index).getText(), index == selectedIndex);
            }
            return suggestionsMap;
        }

        public void selectSuggestion(int index)
        {
            selectedIndex = index;
            if (selectedIndex < 0)
            {
                selectedIndex += suggestions.size();
            }
            if (selectedIndex >= suggestions.size())
            {
                selectedIndex -= suggestions.size();
            }
        }

        public boolean onKeyPressed(int keyCode)
        {
            if (keyCode == UP_KEY_CODE)
            {
                changeSelection(-1);
                changeSelectionOnNextTabInput = false;
                return true;
            } else if (keyCode == DOWN_KEY_CODE)
            {
                changeSelection(1);
                changeSelectionOnNextTabInput = false;
                return true;
            } else if (keyCode == TAB_KEY_CODE)
            {
                if (changeSelectionOnNextTabInput)
                {
                    changeSelection(1);
                }
                applySuggestionToInput();
                return true;
            } else if (keyCode == ESCAPE_KEY_CODE)
            {
                clearSuggestions();
                return true;
            } else
            {
                return false;
            }
        }

        public void applySuggestionToInput()
        {
            Suggestion suggestion = suggestions.get(selectedIndex);
            isApplyingSuggestion = true;
            inputField.setText(suggestion.apply(originalInputText));
            int i = suggestion.getRange().getStart() + suggestion.getText().length();
            inputField.setCaretPosition(i);
            selectSuggestion(selectedIndex);
            isApplyingSuggestion = false;
            changeSelectionOnNextTabInput = true;
        }

        public void changeSelection(int change)
        {
            selectSuggestion(selectedIndex + change);
            int i = lowestDisplayedSuggestionIndex;
            int j = lowestDisplayedSuggestionIndex + maxAmountRendered - 1;
            if (selectedIndex < i)
            {
                lowestDisplayedSuggestionIndex =
                        MathHelper.clamp(selectedIndex, 0, Math.max(suggestions.size() - maxAmountRendered, 0));
            } else if (selectedIndex > j)
            {
                lowestDisplayedSuggestionIndex = MathHelper
                        .clamp(selectedIndex + minAmountRendered - maxAmountRendered, 0,
                                Math.max(suggestions.size() - maxAmountRendered, 0));
            }
        }

        public void clearSuggestions()
        {
            CommandSuggestionHelper.this.suggestions = null;
        }
    }
}
