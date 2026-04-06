package me.user.omnitiny;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

public class OmniTinyMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // Событие: когда любая сущность (включая игрока) загружается в мир
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity.isPlayer()) {
                ScaleData scaleData = ScaleTypes.BASE.getScaleData(entity);
                scaleData.setScale(0.25f); // 0.25 = в 4 раза меньше обычного
            }
        });
    }
}
