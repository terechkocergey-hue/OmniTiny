package me.user.omnitiny;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OmniTinyMod implements ModInitializer {
    public static final String MOD_ID = "omnitiny";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("OmniTiny Mod загружается для Fabric 1.21.1!");

        // Регистрация команд
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("v")
                .executes(context -> {
                    PlayerEntity player = context.getSource().getPlayer();
                    if (player != null) {
                        player.stopRiding();
                        player.sendMessage(Text.literal("§eВы вылезли/встали"), true);
                    }
                    return 1;
                }));
            
            // Здесь будем добавлять /lay, /scale и прочие
        });
    }
}
