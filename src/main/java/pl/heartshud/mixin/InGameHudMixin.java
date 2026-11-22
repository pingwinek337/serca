package pl.heartshud.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.heartshud.HeartsHudClient;

@Mixin(InGameHud.class)
public class InGameHudMixin {

	@Inject(method = "setOverlayMessage", at = @At("HEAD"))
	private void heartshud$onOverlay(Text message, boolean tinted, CallbackInfo ci) {
		HeartsHudClient.onOverlay(message);
	}
}
