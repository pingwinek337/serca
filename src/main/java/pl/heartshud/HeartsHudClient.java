package pl.heartshud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.Locale;
import java.util.UUID;

/**
 * Pokazuje nick i serca OSTATNIEGO gracza, ktorego uderzyles,
 * obok napisu nad hotbarem (action bar, np. "Elytra Hydry").
 */
public class HeartsHudClient implements ClientModInitializer {

	private static UUID lastUuid;
	private static String lastName;
	private static float lastHp = -1f;

	// ustawiane przez mixin (action bar)
	public static Text overlayText;
	public static long overlayUntilMs;

	public static void onOverlay(Text text) {
		overlayText = text;
		overlayUntilMs = System.currentTimeMillis() + 3000;
	}

	@Override
	public void onInitializeClient() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient() && entity instanceof PlayerEntity target && target != player) {
				lastUuid = target.getUuid();
				lastName = target.getName().getString();
				lastHp = target.getHealth() + target.getAbsorptionAmount();
			}
			return ActionResult.PASS;
		});

		// odswiezaj serca ostatnio uderzonego gracza, gdy jest w zasiegu
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world == null || lastUuid == null) return;
			PlayerEntity p = client.world.getPlayerByUuid(lastUuid);
			if (p != null) {
				lastName = p.getName().getString();
				lastHp = p.getHealth() + p.getAbsorptionAmount();
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			lastUuid = null;
			lastName = null;
			lastHp = -1f;
			overlayText = null;
		});

		HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
	}

	private void render(DrawContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden || lastName == null || lastHp < 0) {
			return;
		}

		TextRenderer tr = client.textRenderer;
		int w = context.getScaledWindowWidth();
		int h = context.getScaledWindowHeight();

		Text label = Text.literal(lastName + " ")
				.append(Text.literal("♥ " + formatHearts(lastHp)).styled(s -> s.withColor(0xFF5555)));

		// action bar jest rysowany na y = h - 68, tekst od y = -4 => h - 72
		int y = h - 72;
		int x;
		boolean overlayVisible = overlayText != null && System.currentTimeMillis() < overlayUntilMs;
		if (overlayVisible) {
			x = w / 2 + tr.getWidth(overlayText) / 2 + 8;
		} else {
			x = w / 2 - tr.getWidth(label) / 2;
		}

		context.drawTextWithShadow(tr, label, x, y, 0xFFFFFF);
	}

	private static String formatHearts(float hp) {
		float hearts = hp / 2.0f;
		if (hearts == (int) hearts) {
			return String.valueOf((int) hearts);
		}
		return String.format(Locale.ROOT, "%.1f", hearts);
	}
}
