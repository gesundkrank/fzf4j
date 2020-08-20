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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.googlecode.lanterna.input.KeyType;

import de.gesundkrank.fzf4j.matchers.FuzzyMatcherV1;
import de.gesundkrank.fzf4j.models.OrderBy;
import de.gesundkrank.fzf4j.models.TerminalState;

public class Fzf {

    private final OrderBy orderBy;
    private final boolean reverse;

    private FuzzyMatcherV1 fuzzyMatcherV1;

    public Fzf() {
        this(OrderBy.SCORE, false);
    }

    public Fzf(final OrderBy orderBy, final boolean reverse) {
        this.orderBy = orderBy;
        this.reverse = reverse;
    }

    public List<String> multiSelect(final List<String> items)
            throws AbortByUserException, IOException, EmptyResultException {
        return multiSelect(items, -1);
    }

    public List<String> multiSelect(final List<String> items, final int maxItems)
            throws EmptyResultException, IOException, AbortByUserException {
        final var state = select(items, true, maxItems);
        final var selectedItems = state.getSelectedItems();
        if (selectedItems.isEmpty()) {
            if (state.getCursorItem() == -1) {
                throw new EmptyResultException();
            } else {
                return Collections.singletonList(state.getCursorResult().getText());
            }
        }

        return selectedItems.stream().map(items::get).collect(Collectors.toList());
    }

    /**
     * Starts fzf for a list of strings. Returns a single selected string or throws an exception.
     *
     * @param items list of strings to select from
     * @return selected item
     * @throws IOException          if terminal interaction has an error
     * @throws EmptyResultException if no item was selected
     * @throws AbortByUserException if the user aborts by hitting the escape button
     */
    public String select(final List<String> items)
            throws IOException, EmptyResultException, AbortByUserException {
        final var state = select(items, false, -1);

        if (state.getCursorItem() == -1) {
            throw new EmptyResultException();
        }
        return state.getCursorResult().getText();
    }

    private TerminalState select(
            final List<String> items, final boolean multiSelect, final int maxItems
    )
            throws EmptyResultException, IOException, AbortByUserException {

        if (items == null || items.isEmpty()) {
            throw new EmptyResultException();
        }

        this.fuzzyMatcherV1 = new FuzzyMatcherV1(items, orderBy);

        try (final var view = new View(items, reverse)) {
            final var state = new TerminalState(fuzzyMatcherV1.match(""));
            view.render(state);
            return readInputs(view, state, multiSelect, maxItems);
        }
    }

    private TerminalState readInputs(
            final View view, final TerminalState state, final boolean multiSelect,
            int maxItems
    )
            throws IOException, AbortByUserException {

        var keyStroke = view.readInput();
        var queryBuilder = new StringBuilder();

        while (keyStroke.getKeyType() != KeyType.Escape
               && keyStroke.getKeyType() != KeyType.Enter) {

            final var pageSize = view.pageSize();
            final var numItems = state.getItemsSize();
            var cursorRow = state.getCursorItem();

            switch (keyStroke.getKeyType()) {
                case ArrowDown:
                    final var newSelectedItem =
                            reverse ? up(numItems, cursorRow)
                                    : down(numItems, cursorRow);
                    state.setCursorItem(newSelectedItem);
                    break;
                case ArrowUp:
                    state.setCursorItem(reverse ? down(numItems, cursorRow)
                                                : up(numItems, cursorRow));
                    break;
                case PageDown:
                    state.setCursorItem(reverse ? pageUp(cursorRow, pageSize)
                                                : pageDown(
                                                        state.getItemsSize(),
                                                        cursorRow, pageSize
                                                ));
                    break;
                case PageUp:
                    state.setCursorItem(
                            reverse ? pageDown(state.getItemsSize(), cursorRow, pageSize)
                                    : pageUp(cursorRow, pageSize));
                    break;
                case ArrowLeft:
                    state.setCursorPosition(Math.max(state.getCursorPosition() - 1, 0));
                    break;
                case ArrowRight:
                    state.setCursorPosition(Math.min(
                            state.getCursorPosition() + 1,
                            queryBuilder.length()
                    ));
                    break;
                case Backspace:
                    if (state.getCursorPosition() >= 1) {
                        queryBuilder.deleteCharAt(state.getCursorPosition() - 1);
                        state.setCursorPosition(Math.max(state.getCursorPosition() - 1, 0));
                        state.setResults(fuzzyMatcherV1.match(queryBuilder.toString()));
                        state.setQuery(queryBuilder.toString());
                    }
                    break;
                case Delete:
                    final var cursorPosition = state.getCursorPosition();
                    if (cursorPosition < queryBuilder.length()) {
                        queryBuilder.deleteCharAt(cursorPosition);
                        state.setCursorPosition(
                                Math.min(cursorPosition, queryBuilder.length()));
                        state.setResults(fuzzyMatcherV1.match(queryBuilder.toString()));
                        state.setQuery(queryBuilder.toString());
                    }
                    break;
                case Tab:
                    if (multiSelect) {
                        final var localIndex = state.getCursorItem();
                        final var globalIndex = state.getResults().get(localIndex).getItemIndex();
                        final var selectedItems = state.getSelectedItems();
                        if (selectedItems.contains(globalIndex)) {
                            selectedItems.remove(globalIndex);
                        } else if (maxItems == -1 || selectedItems.size() < maxItems) {
                            selectedItems.add(globalIndex);
                        }
                    }
                    break;
                case Character:
                    if (keyStroke.isCtrlDown()) {
                        if (keyStroke.getCharacter() == 'a') {
                            state.setCursorPosition(0);
                        } else if (keyStroke.getCharacter() == 'e') {
                            state.setCursorPosition(queryBuilder.length());
                        }
                        break;
                    }

                    queryBuilder.insert(state.getCursorPosition(), keyStroke.getCharacter());
                    state.setQuery(queryBuilder.toString());
                    state.setResults(fuzzyMatcherV1.match(queryBuilder.toString()));
                    state.setCursorPosition(state.getCursorPosition() + 1);
                    break;
                default:
            }

            view.render(state);

            keyStroke = view.readInput();
        }

        if (keyStroke.getKeyType() == KeyType.Escape) {
            throw new AbortByUserException();
        }

        return state;
    }

    private int up(final int numItems, final int selectedItem) {
        return (numItems + selectedItem - 1) % numItems;
    }

    private int down(final int numItems, final int selectedItem) {
        return (selectedItem + 1) % numItems;
    }

    private int pageUp(final int selectedItem, final int pageSize) {
        return Math.max(selectedItem - pageSize, 0);
    }

    private int pageDown(final int numItems, final int selectedItem, final int pageSize) {
        return Math.min(selectedItem + pageSize, numItems - 1);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private boolean reverse = false;
        private OrderBy orderBy = OrderBy.SCORE;

        public Fzf build() {
            return new Fzf(orderBy, reverse);
        }

        public Builder reverse() {
            this.reverse = true;
            return this;
        }

        public Builder orderBy(OrderBy orderBy) {
            this.orderBy = orderBy;
            return this;
        }
    }


    public static class AbortByUserException extends Exception {

        public AbortByUserException() {
            super("Interaction aborted by user");
        }
    }

    public static class EmptyResultException extends Exception {

        public EmptyResultException() {
            super("Select result is empty");
        }
    }

}
