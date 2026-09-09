package org.mmga.uglobal.client;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class RpgViewEffects {
    private static boolean recoil;
    private static long shotStarted;
    private RpgViewEffects() {}
    public static boolean isRpg(ItemStack stack) {
        if (!stack.isOf(Items.NETHER_STAR)) return false;
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && "RPG".equals(data.copyNbt()
                .getCompound("PublicBukkitValues").getString("uglobalweapon:special_item"));
    }
    public static void observe(ItemStack item) {
        if (!isRpg(item)) { reset(); return; }
        var model = item.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        int frame=model==null ? 0 : 1000+Math.floorMod(model.value()-1000,100);
        boolean next = frame >= 1020 && frame <= 1023;
        if (next && !recoil) shotStarted = System.nanoTime();
        recoil = next;
    }
    public static double elapsed() {
        return shotStarted == 0 ? 1 : (System.nanoTime() - shotStarted) / 1_000_000_000.0;
    }
    public static void reset() { recoil = false; shotStarted = 0; }
}
