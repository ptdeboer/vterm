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

import java.util.Map;
import java.util.Properties;

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
    private String[] command;
    private Map<String,String> env;

    private final Properties properties=new Properties();

    public void setDefaultSize(int rows, int columns) {
        this.defaultRows = rows;
        this.defaultColumns = columns;
    }

    /**
     * Channel specific options, for example SSH settings.
     */
    public Properties options() {
        return properties;
    }
    public String getOption(String name) {
        return properties.getProperty(name);
    }

    public boolean getBooleanOption(String name, boolean defaultValue) {
        String value = properties.getProperty(name);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public int getIntOption(String name, int defaultValue) {
        String value = properties.getProperty(name);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    public void setOption(String name, boolean value) {
        this.properties.put(name, Boolean.toString(value));
    }

    public void setOption(String name, String value) {
        this.properties.put(name, value);
    }

    public void setOption(String name, int value) {
        this.properties.put(name, Integer.toString(value));
    }
}
