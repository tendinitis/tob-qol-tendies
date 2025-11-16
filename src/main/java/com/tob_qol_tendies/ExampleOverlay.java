package com.tob_qol_tendies;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;
import java.util.Map;

public class ExampleOverlay extends Overlay
{
    private final Client client;
    private final ExampleConfig config;
    private Map<NPC, String> labeledNylos;
    private int bloatCounter = 0;

    private final Font font = new Font("Arial", Font.BOLD, 18);

    @Inject
    private ExampleOverlay(Client client, ExampleConfig config)
    {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGH);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    public void setLabeledNylos(Map<NPC, String> labeledNylos)
    {
        this.labeledNylos = labeledNylos;
    }

    public void setBloatCounter(int counter)
    {
        this.bloatCounter = counter;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // --------------------------
        // 1. Draw Nylocas labels
        // --------------------------
        if (labeledNylos != null && !labeledNylos.isEmpty())
        {
            graphics.setFont(font);
            for (Map.Entry<NPC, String> entry : labeledNylos.entrySet())
            {
                NPC npc = entry.getKey();
                String label = entry.getValue();
                LocalPoint lp = npc.getLocalLocation();
                if (lp == null) continue;

                net.runelite.api.Point textLoc = Perspective.localToCanvas(
                        client, lp, client.getPlane(), graphics.getFontMetrics().getHeight());

                if (textLoc == null) continue;

                // Draw outline
                graphics.setColor(Color.BLACK);
                graphics.drawString(label, textLoc.getX() - 1, textLoc.getY() - 1);
                graphics.drawString(label, textLoc.getX() - 1, textLoc.getY() + 1);
                graphics.drawString(label, textLoc.getX() + 1, textLoc.getY() - 1);
                graphics.drawString(label, textLoc.getX() + 1, textLoc.getY() + 1);

                // Draw text
                graphics.setColor(Color.CYAN);
                graphics.drawString(label, textLoc.getX(), textLoc.getY());
            }
        }

        // --------------------------
        // 2. Draw Bloat Counter
        // --------------------------
        if (bloatCounter > 0)
        {
            int x = config.bloatCounterX();
            int y = config.bloatCounterY();
            int fontSize = config.bloatCounterSize();

            Font bloatFont = new Font("Arial", Font.BOLD, fontSize);
            graphics.setFont(bloatFont);

            String text = String.valueOf(bloatCounter);

            // Text outline
            graphics.setColor(Color.BLACK);
            graphics.drawString(text, x - 2, y - 2);
            graphics.drawString(text, x - 2, y + 2);
            graphics.drawString(text, x + 2, y - 2);
            graphics.drawString(text, x + 2, y + 2);

            // Main text
            graphics.setColor(Color.WHITE);
            graphics.drawString(text, x, y);
        }

        return null;
    }
}
