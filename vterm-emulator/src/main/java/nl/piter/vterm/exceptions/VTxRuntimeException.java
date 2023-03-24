/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.exceptions;

/**
 * Unchecked runtime exception.
 */
public class VTxRuntimeException extends RuntimeException {

    public VTxRuntimeException(String message) {
        super(message);
    }

    public VTxRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public VTxRuntimeException(Throwable cause) {
        super(cause);
    }

}
