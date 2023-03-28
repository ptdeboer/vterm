/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

import static nl.piter.vterm.api.TermConst.XTERM_256COLOR;

/**
 * Generic options.
 */
@Data
@Builder
@AllArgsConstructor
public class TermChannelOptions {

    public static TermChannelOptions create() {
        return TermChannelOptions.builder()
                .defaultColumns(24)
                .defaultRows(24).termType(XTERM_256COLOR)
                .build();
    }

    // fields:
    private int defaultRows;
    private int defaultColumns;
    private String termType;
    private final Map<String, String> options = new HashMap<>();

    public void setDefaultSize(int rows, int columns) {
        this.defaultRows = rows;
        this.defaultColumns = columns;
    }

    /**
     * Channel specific options, for example SSH settings.
     */
    public Map<String, ?> options() {
        return options;
    }

    public boolean getBooleanOption(String name, boolean defaultValue) {
        String value = options.get(name);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public int getIntOption(String name, int defaultValue) {
        String value = options.get(name);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public void setOption(String name, boolean value) {
        this.options.put(name, Boolean.toString(value));
    }

    public void setOption(String name, String value) {
        this.options.put(name, value);
    }

    public void setOption(String name, int value) {
        this.options.put(name, Integer.toString(value));
    }
}
