package org.dsi.ifc.global;

/**
 * VehLog replacement of the stock data class (MHI2_ER_AU57x_K3663_1, lsd.jxe
 * 44688282 bytes, sha1 2be4758b...). Same name, same members, same code as the
 * stock class; the only addition is one call to local.vehlog.VehLog.kick() in
 * each constructor, inside try/catch Throwable, so nothing can reach the caller.
 * The car app creates these objects while it starts, which starts the logger.
 */
public class CarViewOption {
    public int state;
    public int reason;

    public CarViewOption() {
        this.state = 0;
        this.reason = 0;
        try {
            local.vehlog.VehLog.kick();
        } catch (Throwable t) {
            // never let the logger touch the HMI
        }
    }

    public CarViewOption(int n, int n2) {
        this.state = n;
        this.reason = n2;
        try {
            local.vehlog.VehLog.kick();
        } catch (Throwable t) {
            // never let the logger touch the HMI
        }
    }

    public int getState() {
        return this.state;
    }

    public int getReason() {
        return this.reason;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("ViewOption(");
        stringBuffer.append("state:");
        stringBuffer.append(this.state);
        stringBuffer.append(",");
        stringBuffer.append("reason:");
        stringBuffer.append(this.reason);
        stringBuffer.append(")");
        return stringBuffer.toString();
    }
}
