package com.example.mixin;

import com.example.ExampleMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class ExampleMixin {
    @Shadow public HitResult crosshairTarget;

    private boolean wasPrtScnPressed = false;

    @Inject(method = "tick", at = @At("END"))
    private void onTick(CallbackInfo info) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.player != null && client.getWindow() != null) {
            
            // Kollar hårdvarustatus för Print Screen-tangenten
            boolean isPrtScnDown = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_PRINT_SCREEN) == GLFW.GLFW_PRESS;
            
            if (isPrtScnDown && !wasPrtScnPressed) {
                ExampleMod.startStunSlam(client);
                wasPrtScnPressed = true;
            } else if (!isPrtScnDown) {
                wasPrtScnPressed = false;
            }

            // Kör fuskloopen
            ExampleMod.onGameTick(client);
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"))
    private void onManualAttack(CallbackInfoReturnable<Boolean> info) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.player != null && crosshairTarget != null) {
            if (crosshairTarget.getType() == HitResult.Type.ENTITY) {
                ExampleMod.triggerManualSwap();
            }
        }
    }
}
