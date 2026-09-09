package org.mmga.uglobal.client;
/** Only heat-zone sprites get emissive light; cold sprites never do. */
public final class HeatLight {
    public static int minimumBlockLight(String namespace,String path) {
        if (!"uglobalweapon".equals(namespace)) return 0;
        if (path.startsWith("item/energy/") || path.equals("item/palette/redline") || path.equals("item/palette/white")) return 15;
        if (path.startsWith("item/heat/3/")) return 12;
        if (path.startsWith("item/heat/2/")) return 10;
        if (path.startsWith("item/heat/1/")) return 7;
        return 0;
    }
}
