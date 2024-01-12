/*
 * Copyright (c) 2020 Jan Graßegger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.gesundkrank.fzf4j;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import de.gesundkrank.fzf4j.models.TerminalColors;
import de.gesundkrank.fzf4j.models.TerminalState;

/**
 * Responsible for drawing everything to the terminal
 */
public class View implements AutoCloseable {

    public static final int CHECK_RESIZE_INTERVAL_MS = 100;

    private final Screen screen;
    private final int totalItems;
    private final boolean reverse;
    private final ScheduledExecutorService executor;
    private final TerminalColors terminalColors;

    /**
     * Marks the beginning of the drawing window if more items exist than can be shown
     */
    private int drawStart = 0;

    private volatile TerminalState state;

    public View(
            final List<String> items,
            final boolean reverse,
            final TerminalColors terminalColors
    ) throws IOException {
        this.terminalColors = terminalColors;
        var defaultTerminalFactory = new DefaultTerminalFactory();
        this.screen = defaultTerminalFactory.createScreen();

        this.totalItems = items.size();
        this.reverse = reverse;

        executor = Executors.newSingleThreadScheduledExecutor();

        executor.scheduleAtFixedRate(
                this::renderIfResized, CHECK_RESIZE_INTERVAL_MS, CHECK_RESIZE_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );

        screen.startScreen();
    }

    public int pageSize() {
        return screen.getTerminalSize().getRows() - 3;
    }

    public KeyStroke readInput() throws IOException {
        return screen.readInput();
    }

    private void renderIfResized() {
        if (state != null && screen.doResizeIfNecessary() != null) {
            try {
                render();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void render(final TerminalState state) throws IOException {
        this.state = state;
        render();
    }

    private synchronized void render() throws IOException {
        screen.clear();

        final int rows = screen.getTerminalSize().getRows();
        final int itemsSize = state.getItemsSize();
        final int itemRows = rows - 2;
        final int itemCount = Math.min(itemRows, itemsSize);
        final int selectedPosition = state.getCursorItem();

        if (itemsSize < itemRows) {
            drawStart = 0;
        } else if (selectedPosition < drawStart) {
            drawStart = selectedPosition;
        } else if (selectedPosition >= drawStart + itemCount) {
            drawStart = (selectedPosition - itemCount + 1) % itemsSize;
        }

        drawStart = Math.max(Math.min(drawStart, itemsSize - itemCount), 0);

        final var visibleItems = itemsSize > itemCount
                                 ? state.getResults().subList(drawStart, drawStart + itemCount)
                                 : state.getResults();

        final var localSelectedItem = selectedPosition - drawStart;

        IntStream.range(0, itemRows).forEach(row -> {
            final var itemIndex = reverse ? itemRows - (row + 1) : row;

            final var textGraphics = screen.newTextGraphics();

            if (itemIndex < visibleItems.size()) {
                final var item = visibleItems.get(itemIndex);
                final var text = item.getText();
                final var positions = item.getPositions();
                final var backgroundColor = itemIndex == localSelectedItem
                                            ? terminalColors.getMarkerBackgroundColor()
                                            : TextColor.ANSI.DEFAULT;
                var posIndex = 0;

                if (itemIndex == localSelectedItem) {
                    screen.setCharacter(
                            0, row, TextCharacter.fromCharacter(
                                    '>',
                                    terminalColors.getMarkerItemColor(),
                                    backgroundColor
                            )[0]);
                    screen.setCharacter(
                            1, row, TextCharacter.fromCharacter(
                                    ' ',
                                    terminalColors.getMarkerItemColor(),
                                    backgroundColor
                            )[0]);
                } else {
                    screen.setCharacter(0, row, TextCharacter.fromCharacter(' ')[0]);
                }

                if (state.getSelectedItems().contains(item.getItemIndex())) {
                    screen.setCharacter(
                            1, row, TextCharacter.fromCharacter(
                                    '>',
                                    terminalColors.getSelectedItemColor(),
                                    backgroundColor
                            )[0]);
                }

                for (var i = 0; i < item.getText().length(); i++) {
                    final TextColor textColor;
                    if (positions != null && posIndex < positions.length
                        && i == positions[posIndex]) {
                        textColor = terminalColors.getMatchedCharsColor();
                        posIndex++;
                    } else {
                        textColor = TextColor.ANSI.DEFAULT;
                    }

                    final TextCharacter character;

                    if (itemIndex == localSelectedItem) {
                        character = TextCharacter.fromCharacter(
                                text.charAt(i), textColor, backgroundColor, SGR.BOLD
                        )[0];
                    } else {
                        character = TextCharacter.fromCharacter(text.charAt(i), textColor, backgroundColor)[0];
                    }

                    textGraphics.setCharacter(2 + i, row, character);

                }
            }
        });

        screen.newTextGraphics()
                .putString(0, rows - 2, String.format("  %d/%d", itemsSize, totalItems));

        final var textGraphics = screen.newTextGraphics();
        textGraphics.putString(new TerminalPosition(0, rows - 1), "> " + state.getQuery());
        screen.setCursorPosition(new TerminalPosition(state.getCursorPosition() + 2, rows - 1));
        screen.refresh();
    }

    @Override
    public void close() throws IOException {
        executor.shutdownNow();
        screen.stopScreen();
        screen.close();
    }
}
