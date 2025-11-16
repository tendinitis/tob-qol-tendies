package com.tob_qol_tendies;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.sound.sampled.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@PluginDescriptor(
        name = "tob_qol_tendies"
)
public class ExamplePlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ExampleConfig config;
    @Inject private OverlayManager overlayManager;
    @Inject private ExampleOverlay overlay;

    private NPC maiden = null;
    private final Map<NPC, String> labeledNylos = new HashMap<>();
    private NPC bloat = null;

    private static final Set<Integer> MAIDEN_IDS = Set.of(10814, 8360, 10822);
    private static final Set<Integer> NYLOCAS_IDS = Set.of(10820, 8366, 10828);
    private static final Set<Integer> BLOAT_IDS = Set.of(10812, 8359, 10813);
    private static final Set<Integer> VERZIK_IDS = Set.of(10835,10836,8374,8375,10852,10853);

    // ---------------- Bloat counter ----------------
    private boolean counterActive = false;
    private boolean counterTriggered = false;
    private boolean attackRegistered = false;
    private boolean pendingPause = false;
    private int pauseDelayTicks = 0;
    private boolean skipIncrementThisTick = false;
    private int counterTicks = 0;
    private int lifetimeTicks = 0;
    private static final int MAX_LIFETIME_TICKS = 33;
    private static final int MAX_COUNTER = 5;

    // ---------------- Verzik enrage ----------------
    private boolean verzikEnrageTriggered = false;
    private int verzikSoundTicks = 0;

    @Override
    protected void startUp()
    {
        overlay.setLabeledNylos(labeledNylos);
        overlay.setBloatCounter(0);
        overlayManager.add(overlay);
        verzikEnrageTriggered = false;
        verzikSoundTicks = 0;
        log.info("[Startup] Example plugin started!");
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        labeledNylos.clear();
        maiden = null;
        bloat = null;
        resetBloatCounter();
        verzikEnrageTriggered = false;
        verzikSoundTicks = 0;
        log.info("[Shutdown] Example plugin stopped!");
    }

    // ---------------- NPC Spawn/Despawn ----------------
    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        NPC npc = event.getNpc();
        if (npc == null || npc.getComposition() == null) return;

        int id = npc.getComposition().getId();
        log.debug("[NpcSpawned] NPC {} spawned (ID {})", npc.getName(), id);

        if (MAIDEN_IDS.contains(id) || NYLOCAS_IDS.contains(id))
            handleMaidenAndNylosSpawn(npc);
        else if (BLOAT_IDS.contains(id))
            handleBloatSpawn(npc);
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        NPC npc = event.getNpc();
        if (npc == null) return;

        labeledNylos.remove(npc);
        log.debug("[NpcDespawned] NPC {} despawned", npc.getName());

        if (npc == bloat)
        {
            bloat = null;
            resetBloatCounter();
            log.info("[NpcDespawned] Pestilent Bloat despawned. Counter reset.");
        }
    }

    // ---------------- GameTick ----------------
    @Subscribe
    public void onGameTick(GameTick event)
    {
        // ---------------- Bloat counter ----------------
        if (pendingPause)
        {
            pauseDelayTicks--;
            log.debug("[GameTick] Pending pause countdown: {}", pauseDelayTicks);
            if (pauseDelayTicks <= 0)
            {
                pendingPause = false;
                if (counterActive)
                {
                    counterActive = false;
                    log.info("[GameTick] Counter paused one tick after attack registered.");
                }
            }
        }

        if (bloat != null)
        {
            int animId = bloat.getAnimation();
            log.debug("[GameTick] Bloat animation ID: {}", animId);

            if (animId == 8082 && !counterTriggered)
            {
                counterActive = true;
                counterTriggered = true;
                counterTicks = 1;
                lifetimeTicks = 0;
                attackRegistered = false;
                pendingPause = false;
                pauseDelayTicks = 0;
                skipIncrementThisTick = true;
                log.info("[GameTick] Bloat 8082 animation triggered counter.");
            }

            if (counterTriggered)
            {
                lifetimeTicks++;
                log.debug("[GameTick] Bloat lifetimeTicks: {}", lifetimeTicks);

                if (counterActive && !skipIncrementThisTick)
                {
                    counterTicks++;
                    if (counterTicks > MAX_COUNTER) counterTicks = 1;
                    log.debug("[GameTick] Bloat counterTicks incremented: {}", counterTicks);
                }

                if (lifetimeTicks >= MAX_LIFETIME_TICKS)
                {
                    log.info("[GameTick] Counter expired after {} ticks.", lifetimeTicks);
                    resetBloatCounter();
                }
            }

            overlay.setBloatCounter(counterTicks);

            if (skipIncrementThisTick) skipIncrementThisTick = false;
        }

        // ---------------- Verzik enrage detection ----------------
        for (NPC npc : client.getNpcs())
        {
            if (!VERZIK_IDS.contains(npc.getComposition().getId())) continue;

            String overheadText = npc.getOverheadText();
            log.debug("[GameTick] Verzik NPC {} overhead text: {}", npc.getName(), overheadText);

            if (overheadText != null &&
                    overheadText.contains("I'm not finished with you just yet!") &&
                    !verzikEnrageTriggered)
            {
                verzikEnrageTriggered = true;
                verzikSoundTicks = 3; // Wait 3 ticks before playing
                log.info("[GameTick] Verzik enrage detected! Will play sound in {} ticks.", verzikSoundTicks);
            }
        }

// ---------------- Verzik sound delay ----------------
        if (verzikEnrageTriggered)
        {
            if (verzikSoundTicks > 0)
            {
                verzikSoundTicks--;
                log.debug("[GameTick] Verzik sound delay countdown: {}", verzikSoundTicks);
            }
            else if (verzikSoundTicks == 0)
            {
                playVerzikWav(); // play verzik2.wav once
                verzikEnrageTriggered = false; // reset trigger
                log.info("[GameTick] Verzik sound played after delay.");
            }
        }

    }

    // ---------------- Player attack detection ----------------
    @Subscribe
    public void onAnimationChanged(AnimationChanged event)
    {
        if (client.getLocalPlayer() == null) return;
        if (event.getActor() != client.getLocalPlayer()) return;
        if (bloat == null || !counterActive || attackRegistered) return;

        Actor interacting = client.getLocalPlayer().getInteracting();
        if (interacting == bloat)
        {
            pendingPause = true;
            pauseDelayTicks = 2;
            attackRegistered = true;
            log.info("[AnimationChanged] Player attack detected. Counter will pause next tick.");
        }
    }

    // ---------------- Boss handlers ----------------
    private void handleMaidenAndNylosSpawn(NPC npc)
    {
        int id = npc.getComposition().getId();
        WorldPoint wp = npc.getWorldLocation();

        if (MAIDEN_IDS.contains(id))
        {
            maiden = npc;
            log.info("[Spawn] Maiden spawned at {},{}", wp.getX(), wp.getY());
            return;
        }

        if (NYLOCAS_IDS.contains(id) && maiden != null)
        {
            WorldPoint maidenLoc = maiden.getWorldLocation();
            int maidenX = maidenLoc.getX();
            int maidenY = maidenLoc.getY();

            WorldPoint s1 = new WorldPoint(maidenX + 11, maidenY - 8, maidenLoc.getPlane());
            WorldPoint s2 = new WorldPoint(maidenX + 15, maidenY - 8, maidenLoc.getPlane());
            WorldPoint n1 = new WorldPoint(maidenX + 11, maidenY + 12, maidenLoc.getPlane());
            WorldPoint n2 = new WorldPoint(maidenX + 15, maidenY + 12, maidenLoc.getPlane());

            String label = null;
            if (wp.equals(s1)) label = "S1";
            else if (wp.equals(s2)) label = "S2";
            else if (wp.equals(n1)) label = "N1";
            else if (wp.equals(n2)) label = "N2";

            if (label != null)
            {
                labeledNylos.put(npc, label);
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", label + " Nylocas spawned!", null);
                log.info("[Spawn] {} Nylocas spawned at ({}, {})", label, wp.getX(), wp.getY());
            }
        }
    }

    private void handleBloatSpawn(NPC npc)
    {
        bloat = npc;
        log.info("[Spawn] Pestilent Bloat spawned at ({}, {})", npc.getWorldLocation().getX(), npc.getWorldLocation().getY());
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Pestilent Bloat detected!", null);
    }

    private void resetBloatCounter()
    {
        counterActive = false;
        counterTriggered = false;
        attackRegistered = false;
        pendingPause = false;
        pauseDelayTicks = 0;
        skipIncrementThisTick = false;
        counterTicks = 0;
        lifetimeTicks = 0;
        overlay.setBloatCounter(0);
        log.debug("[Reset] Bloat counter reset.");
    }

    private void playVerzikWav()
    {
        try
        {
            File file = new File("verzik" + File.separator + "verzik2.wav");
            if (!file.exists())
            {
                log.warn("[VerzikSound] File not found: {}", file.getAbsolutePath());
                return;
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
            log.debug("[VerzikSound] Playing file: {}", file.getName());
        }
        catch (Exception e)
        {
            log.error("[VerzikSound] Failed to play WAV: ", e);
        }
    }

    // ---------------- Config ----------------
    @Provides
    ExampleConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ExampleConfig.class);
    }
}
