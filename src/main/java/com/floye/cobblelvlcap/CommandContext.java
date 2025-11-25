package com.floye.cobblelvlcap;

import com.floye.cobblelvlcap.config.CapConfig;

public final class CommandContext {
    private static final ThreadLocal<Boolean> FLAG = ThreadLocal.withInitial(() -> false);

    public static void enter() { if (CapConfig.CFG.allowCommandBypass) FLAG.set(true); }
    public static void exit()  { if (CapConfig.CFG.allowCommandBypass) FLAG.set(false); }
    public static boolean inCommand() { return CapConfig.CFG.allowCommandBypass && Boolean.TRUE.equals(FLAG.get()); }
}