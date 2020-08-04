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

package de.gesundkrank.fzf4j.models;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TerminalState {

    private int cursorItem = 0;
    private int cursorPosition = 0;
    private String query = "";
    private List<Result> results;
    private Set<Integer> selectedItems = new HashSet<>();

    public TerminalState(final List<Result> results) {
        this.results = results;
    }

    public int getCursorItem() {
        return cursorItem;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public String getQuery() {
        return query;
    }

    public List<Result> getResults() {
        return results;
    }

    public int getItemsSize() {
        return results.size();
    }

    public void setCursorItem(int cursorItem) {
        this.cursorItem = cursorItem;
    }

    public void setCursorPosition(int cursorPosition) {
        this.cursorPosition = cursorPosition;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setResults(List<Result> results) {
        this.results = results;
        this.cursorItem = results.size() == 0
                            ? -1
                            : Math.min(cursorItem, results.size() - 1);
    }

    public Set<Integer> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(Set<Integer> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public Result getCursorResult() {
        return results.get(cursorItem);
    }
}
