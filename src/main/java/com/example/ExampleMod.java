package com.example;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.Hand;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import java.util.Random;

public class ExampleMod {
    private static final Random random = new Random();
    
    // Konfiguration låst till dina exakta hotbar-slots (Siffra i spelet minus 1)
    private static final int SWORD_SLOT = 0;        // Slot 1 i spelet
    private static final int SPEAR_SLOT = 8;        // Slot 9 i spelet
    private static final int AXE_SLOT = 5;          // Slot 6 i spelet
    private static final int DENSITY_MACE_SLOT = 2; // Slot 3 i spelet
    private static final int BREACH_MACE_SLOT = 7;  // Slot 8 i spelet

    // Triggerbot inställningar
    private static boolean wasTargetingEntity = false;
    private static boolean queueAirHit = false;
    private static int airHitDelayTicks = 0;
    
    // Spam-klick inställningar (för 1.8 / Hypixel)
    private static long lastSpamAttackTime = 0;
    private static long currentSpamDelayMs = 0;

    // Spear-swap & Auto-Reset inställningar
    private static boolean isSwapPending = false;
    private static int swapDelayTicks = 0;
    private static boolean isResetPending = false;
    private static int resetDelayTicks = 0;

    // Stun Slam inställningar
    private static boolean isSlamSequenceActive = false;
    private static int slamStage = 0;
    private static int slamDelayTicks = 0;
    private static int activeMaceSlot = BREACH_MACE_SLOT;

    public static void onGameTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        // --- 1. STUN SLAM MACRO SEKVENSHANTERING ---
        if (isSlamSequenceActive) {
            slamDelayTicks--;
            if (slamDelayTicks = currentSpamDelayMs) {
                            client.interactionManager.attackEntity(client.player, target);
                            client.player.swingHand(Hand.MAIN_HAND);
                            wasTargetingEntity = true;
                            
                            lastSpamAttackTime = currentTime;
                            currentSpamDelayMs = random.nextInt(21) + 70; 
                        }
                    } else {
                        // 1.9+ Attack-logik (Slår automatiskt när cooldown nått 100%)
                        if (client.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                            client.interactionManager.attackEntity(client.player, target);
                            client.player.swingHand(Hand.MAIN_HAND);
                            wasTargetingEntity = true;
                        }
                    }
                }
            } else {
                if (wasTargetingEntity) {
                    if (random.nextFloat() < 0.30f) { // 30% risk för "panic air hit" för humanisering
                        queueAirHit = true;
                        airHitDelayTicks = random.nextInt(2) + 1;
                    }
                    wasTargetingEntity = false;
                }
            }
        }

        // --- 4. TRIGGERBOT AIR HIT MISSA MED FLIT ---
        if (queueAirHit) {
            airHitDelayTicks--;
            if (airHitDelayTicks <= 0) {
                client.player.swingHand(Hand.MAIN_HAND);
                queueAirHit = false;
            }
        }
    }

    // Aktiveras när Mixin känner av tryck på Print Screen-knappen
    public static void startStunSlam(MinecraftClient client) {
        if (!isSlamSequenceActive && !isSwapPending && !isResetPending) {
            HitResult hitResult = client.crosshairTarget;
            
            if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                Entity target = ((EntityHitResult) hitResult).getEntity();
                
                if (target instanceof LivingEntity livingTarget) {
                    // Säkra att målet faktiskt skyddar sig med en sköld
                    boolean hasShield = livingTarget.isHolding(Items.SHIELD);
                    
                    if (hasShield) {
                        // 15-20% risk för misslyckat macro ("human choke")
                        if (random.nextFloat() < 0.17f) {
                            client.player.swingHand(Hand.MAIN_HAND);
                            return; 
                        }

                        // Välj Mace-typ dynamiskt baserat på din nuvarande fallhöjd
                        float fallHeight = client.player.fallDistance;
                        if (fallHeight > 8.0f) {
                            activeMaceSlot = DENSITY_MACE_SLOT; // Höga drop-angrepp
                        } else {
                            activeMaceSlot = BREACH_MACE_SLOT;  // Standard mark-strid
                        }

                        isSlamSequenceActive = true;
                        slamStage = 1;

                        // Steg 1: Bryt skölden med yxan
                        executeHotbarSwap(client, AXE_SLOT);
                        client.player.swingHand(Hand.MAIN_HAND);

                        // Slumpmässig fördröjning (2 eller 3 ticks) innan mace-slaget
                        slamDelayTicks = random.nextInt(2) + 2;
                    }
                }
            }
        }
    }

    private static void advanceSlamSequence(MinecraftClient client) {
        if (slamStage == 1) {
            // Steg 2: Slå med den taktiskt valda mace-varianten
            executeHotbarSwap(client, activeMaceSlot);
            client.player.swingHand(Hand.MAIN_HAND);
            
            slamStage = 2;
            slamDelayTicks = 2; 
        } 
        else if (slamStage == 2) {
            // Steg 3: Återvänd ljudlöst till svärdet
            executeHotbarSwap(client, SWORD_SLOT);
            isSlamSequenceActive = false;
            slamStage = 0;
        }
    }

    // Aktiveras av Mixin enbart vid dina fysiska left-clicks
    public static void triggerManualSwap() {
        if (!isSwapPending && !isResetPending && !isSlamSequenceActive) {
            swapDelayTicks = random.nextInt(3) + 2; // Slumpmässiga 2-4 ticks delay
            isSwapPending = true;
        }
    }

    private static void executeHotbarSwap(MinecraftClient client, int slot) {
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            client.player.getInventory().selectedSlot = slot;
        }
    }
}
