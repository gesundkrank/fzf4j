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

import com.googlecode.lanterna.TextColor;

public class TerminalColors {

    public static final TextColor DEFAULT_MARKER_ITEM_COLOR = TextColor.ANSI.RED;
    public static final TextColor DEFAULT_MARKER_BG_COLOR = new TextColor.Indexed(237);
    public static final TextColor DEFAULT_MATCHED_CHARS_COLOR = new TextColor.Indexed(70);
    public static final TextColor DEFAULT_SELECTED_ITEM_COLOR = new TextColor.Indexed(1);

    public static TerminalColors DEFAULT_COLORS =
            new TerminalColors(DEFAULT_MARKER_ITEM_COLOR,
                               DEFAULT_MARKER_BG_COLOR, DEFAULT_MATCHED_CHARS_COLOR,
                               DEFAULT_SELECTED_ITEM_COLOR
            );

    private final TextColor markerItemColor;
    private final TextColor markerBackgroundColor;
    private final TextColor matchedCharsColor;
    private final TextColor selectedItemColor;

    public TerminalColors(
            TextColor markerItemColor,
            TextColor markerBackgroundColor,
            TextColor matchedCharsColor,
            TextColor selectedItemColor
    ) {
        this.markerItemColor = markerItemColor;
        this.markerBackgroundColor = markerBackgroundColor;
        this.matchedCharsColor = matchedCharsColor;
        this.selectedItemColor = selectedItemColor;
    }

    public TextColor getMarkerItemColor() {
        return markerItemColor;
    }

    public TextColor getMarkerBackgroundColor() {
        return markerBackgroundColor;
    }

    public TextColor getMatchedCharsColor() {
        return matchedCharsColor;
    }

    public TextColor getSelectedItemColor() {
        return selectedItemColor;
    }
}
