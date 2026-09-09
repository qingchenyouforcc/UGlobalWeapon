package org.mmga.uglobal.utils;
public final class WeaponModel {
    public static int frame(int model) { return 1000+Math.floorMod(model-1000,100); }
    public static int heat(int model) { return Math.max(0,Math.min(3,(model-1000)/100)); }
    public static int withHeat(int model,int heat) { return frame(model)+Math.max(0,Math.min(3,heat))*100; }
    public static int coolingTier(double remaining) { return remaining<=0 ? 0 : remaining>.66 ? 3 : remaining>.33 ? 2 : 1; }
}
