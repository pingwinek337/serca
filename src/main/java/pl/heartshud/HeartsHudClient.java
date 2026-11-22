package pl.heartshud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

/**
 * Rysuje obok nazwy trzymanego itemu (np. "Elytra Hydry") nick gracza i ilosc jego serc.
 * Gracz = ten, na ktorego patrzysz; jesli na nikogo nie patrzysz - najblizszy gracz w zasiegu.
 */
public class HeartsHudClient implements ClientModInitializer {

	private static final double MAX_DISTANCE = 30.0;

	@Override
	public void onInitializeClient() {
		HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
	}

	private void render(DrawContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null || client.options.hudHidden) {
			return;
		}

		PlayerEntity target = findTarget(client);
		if (target == null) {
			return;
		}

		float hp = target.getHealth() + target.getAbsorptionAmount();
		String hearts = formatHearts(hp);

		TextRenderer tr = client.textRenderer;
		int screenW = context.getScaledWindowWidth();
		int screenH = context.getScaledWindowHeight();

		// pozycja nazwy itemu nad hotbarem (jak w vanilla InGameHud)
		int y = screenH - 59;
		if (client.interactionManager != null && !client.interactionManager.hasStatusBars()) {
			y += 14;
		}

		Text itemName = client.player.getMainHandStack().getName();
		int nameWidth = client.player.getMainHandStack().isEmpty() ? 0 : tr.getWidth(itemName);
		int x = screenW / 2 + nameWidth / 2 + 8;

		Text label = Text.literal(target.getName().getString() + " ")
				.append(Text.literal("❤ " + hearts).styled(s -> s.withColor(0xFF5555)));

		context.drawTextWithShadow(tr, label, x, y, 0xFFFFFF);
	}

	private PlayerEntity findTarget(MinecraftClient client) {
		Entity looked = client.targetedEntity;
		if (looked instanceof PlayerEntity p && p != client.player) {
			return p;
		}

		PlayerEntity nearest = null;
		double best = MAX_DISTANCE * MAX_DISTANCE;
		for (PlayerEntity p : client.world.getPlayers()) {
			if (p == client.player) continue;
			double d = p.squaredDistanceTo(client.player);
			if (d < best) {
				best = d;
				nearest = p;
			}
		}
		return nearest;
	}

	private String formatHearts(float hp) {
		float hearts = hp / 2.0f;
		if (hearts == (int) hearts) {
			return String.valueOf((int) hearts);
		}
		return String.format(java.util.Locale.ROOT, "%.1f", hearts);
	}
}
