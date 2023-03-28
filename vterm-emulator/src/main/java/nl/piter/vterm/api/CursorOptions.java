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
