package fun.rich.mixins.client.screen.ingame;

import fun.rich.Rich;
import fun.rich.features.impl.misc.KTLeave;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, CallbackInfo ci) {
        String raw = message.getString();
        if (raw != null) {
            String lower = raw.toLowerCase();

            // Старый фильтр "death position"
            if (lower.contains("death position")) {
                ci.cancel();
                return;
            }

            // Сообщение с кнопкой "[Телепортироваться досрочно]"
            if (raw.contains("[Телепортироваться досрочно]")) {
                KTLeave ktLeave = Rich.getInstance().getModuleProvider().get(KTLeave.class);
                if (ktLeave != null) {
                    ktLeave.notifyTeleportPrompt();
                }
            }
        }
    }
}
