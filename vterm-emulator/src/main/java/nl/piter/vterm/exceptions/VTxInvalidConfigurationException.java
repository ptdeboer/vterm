/*
 * (C) 2005 - 2012 Virtual Laboratory for eScience (VL-e).
 * (C) 2012 - 2015 Netherlands eScience Center.
 * (C) 2005 - 2023 Piter.NL
 *     See LICENSE.txt for details.
 */
//---
package nl.piter.vterm.exceptions;

public class VTxInvalidConfigurationException extends VTxRuntimeException {

    public VTxInvalidConfigurationException(String message) {
        super(message);
    }

    public VTxInvalidConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public VTxInvalidConfigurationException(Throwable cause) {
        super(cause);
    }
}
