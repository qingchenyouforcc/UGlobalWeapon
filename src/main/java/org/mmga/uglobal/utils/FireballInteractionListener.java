package org.mmga.uglobal.utils;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;
import org.mmga.uglobal.Weapon;

import java.util.List;
import java.util.Objects;

public class FireballInteractionListener implements Listener {
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        List<MetadataValue> metadataList = entity.getMetadata("Weapon");
        String customValue = null;

        for (MetadataValue value : metadataList) {
            // 只处理由自己的插件设置的元数据
            if (Objects.equals(value.getOwningPlugin(), Weapon.getInstance())) {
                customValue = value.asString();
                // 现在你可以使用 customValue 了
                break; // 如果只需要一个，那就中断循环
            }
        }

        // 如果点击的实体是我们通过某种方式识别的火球（例如自定义名称或其他标记）
        if (entity instanceof Fireball && Objects.equals(customValue, "Nuclear")) {
            event.setCancelled(true);
            ((Fireball) entity).setDirection(new Vector(0, -1, 0));
            System.out.println("取消成功");
        }
    }
}
