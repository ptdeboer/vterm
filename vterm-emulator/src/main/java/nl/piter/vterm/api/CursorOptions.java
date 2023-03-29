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

@Data
@Builder
@AllArgsConstructor
public class CursorOptions {

    public int x;
    public int y;
    public boolean enabled;
    public boolean blink;

    public boolean enabled() {
        return enabled;
    }

    public boolean blink() {
        return blink;
    }

}
