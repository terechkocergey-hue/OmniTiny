package me.user.omnitiny;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

public class OmniTinyMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            // Проверяем: это игрок И его имя совпадает с твоим?
            if (entity.isPlayer() && entity.getName().getString().equals("AkiSeen")) {
                ScaleData scaleData = ScaleTypes.BASE.getScaleData(entity);
                scaleData.setScale(0.25f); // Только ты будешь маленьким
            } else if (entity.isPlayer()) {
                // Все остальные остаются обычного размера (масштаб 1.0)
                ScaleData scaleData = ScaleTypes.BASE.getScaleData(entity);
                scaleData.setScale(1.0f);
            }
        });
    }
}
