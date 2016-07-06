package aerodude30.com;

import org.powerbot.script.Area;
import org.powerbot.script.Condition;
import org.powerbot.script.PaintListener;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Random;
import org.powerbot.script.Script;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.Bank;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Npc;

import java.awt.*;
import java.util.concurrent.Callable;

/**
 * Created by cbartram on 7/6/2016.
 */
@Script.Manifest(name = "AeroEssence", properties = "author=aerodude30; topic=1296203; client=4;", description = "Mines Rune Essence in east Varrock mine. Supports Bronze-dragon pickaxes.")
public class AeroEssence extends PollingScript<ClientContext> implements PaintListener {

    public Controller controller = ctx.controller;
    private Area varrockBank = new Area(new Tile(3250, 3245), new Tile(3258, 3416));
    private Area auburyHut = new Area(new Tile(3251, 3405), new Tile(3254, 3399));
    private static final int[] PICKAXE = {1265, 1267, 1269, 1271, 1273, 1275, 15259};
    private static final Tile[] pathToAubury = {
            new Tile(3254, 3421, 0),
            new Tile(3254, 3426, 0),
            new Tile(3256, 3428, 0),
            new Tile(3260, 3430, 0),
            new Tile(3263, 3417, 0),
            new Tile(3258, 3408, 0),
            new Tile(3256, 3399, 0),
            new Tile(3253, 3402, 0)
    };

    private static final int RUNE_ESSENCE = 1436, AUBURY_DOOR = 9999, AUBURY_NPC = 9999;//todo change to door id
    private long startTime;
    private String status = "Waiting to start...";
    private int startExperience = 0;

    //Class instances
    private Random rnd = new Random();
    private Util util = new Util();

    //enums for each state in the script
    private enum State {BANK, TRAVERSE, MINE, REVERSE}

    private State getState() {

        //if player is in the bank and they have more than one rune essence
        if(varrockBank.contains(ctx.players.local().tile()) && ctx.inventory.select().id(RUNE_ESSENCE).count() >= 1) {
            return State.BANK;
        }

        //if the player has a pickaxe in their inventory, or equipped, their backpack does not have Rune ess, and they are standing in the bank
        if((ctx.inventory.select().id(PICKAXE).count() == 1 || ctx.equipment.select().id(PICKAXE).count() == 1) &&
                !ctx.inventory.select().contains(ctx.inventory.select().id(RUNE_ESSENCE).poll()) &&
                 varrockBank.contains(ctx.players.local().tile())) {
                return State.TRAVERSE;
        }

        //if player has a full inventory of rune essence or a full inventory - 1 because of the pickaxe in their inventory
        if(ctx.inventory.select().id(RUNE_ESSENCE).count() == 28 || ctx.inventory.select().id(RUNE_ESSENCE).count() == 27) {
            return State.REVERSE;
        } else {
            return State.MINE;
        }
    }



    @Override
    public void start() {
        startTime = System.currentTimeMillis();
        startExperience = ctx.skills.experience(14);
    }


    @Override
    public void poll() {
        State state = getState();

        switch(state) {
            case BANK:
                status = "Banking...";
                if(ctx.bank.inViewport() && !ctx.bank.opened()) {
                    ctx.camera.turnTo(ctx.npcs.select().id(2897).shuffle().poll());

                    //Anti-Pattern Feature to sometimes choose to open the bank booth and sometimes choose to interact with the banker himself
                    Boolean bankOrBanker = Random.nextBoolean();

                    if (bankOrBanker) {
                        ctx.bank.open();
                    } else {
                        status = "Interacting with Banker NPC";
                        final Npc banker = ctx.npcs.select().id(2897).shuffle().poll();
                        ctx.camera.turnTo(banker);
                        banker.interact("Bank", banker.name());
                    }
                    status = "Depositing Essence...";
                    if (ctx.inventory.count() != 0) {
                       ctx.bank.deposit(RUNE_ESSENCE, Bank.Amount.ALL);
                    }

                  ctx.bank.close();
                }
                    break;
            case TRAVERSE:
                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        status = "Walking to Aubury";
                      return ctx.movement.newTilePath(pathToAubury).traverse();
                    }
                }, 3000, 10);

                final Npc aubury = ctx.npcs.select().id(AUBURY_NPC).poll();
                status = "Teleporting from Aubury";
                //todo if the door is closed open it else interact with Aubury
                if(ctx.objects.select().id(AUBURY_DOOR).poll().contains(new Point())) {

                    ctx.objects.select().id(AUBURY_DOOR).poll().click();
                    aubury.interact("Teleport", aubury.name());
                } else {
                    aubury.interact("Teleport", aubury.name());
                }

                break;
            case MINE:
                //if rune rock is in view interact
                    //else
                //move closer

                //while player animation is mining
                    //Condition.wait() run antipattern (check your stats, move the camera, examine a widget etc...

                break;
            case REVERSE:
                //if portal is in view interact with it
                    //else
                //move closer to the portal then interact with it

                //if player is in Aubury's hut
                    //ctx.movement.newTilePath(pathToAubury).reverse();
                break;
        }
    }


    @Override
    public void repaint(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        int expGained = ctx.skills.experience(14) - startExperience;

        g.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF));
        g.setColor(new Color(136, 136, 136, 117));
        g.fillRect(3, 3, 175, 145);
        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(51, 153, 255));
        g.drawRect(3, 3, 175, 145);
        g.setColor(new Color(51, 153, 255));
        g.drawLine(12, 31, 134, 31);
        g.setColor(new Color(255, 255, 255));
        g.setFont(new Font("Arial", 0, 14));
        g.drawString("AeroEssence", 19, 25);
        g.setColor(new Color(255, 255, 255));
        g.setFont(new Font("Arial", 0, 11));
        g.drawString("Time Running: " , 13, 48);
        g.drawString("Mining Exp Gained: ", 14, 65);
        g.drawString("Mining/hour: ", 14, 84);
        g.drawString("Starting Level: ", 15, 103);
        g.drawString("Current Level: ", 15, 123);
        g.drawString("Status: ", 16, 141);
        g.drawString(util.runtime(startTime), 85, 49);
        g.drawString(String.valueOf(expGained), 120, 65);
        g.drawString(util.perHour(expGained, startExperience) + " (" + util.formatNumber(expGained) + ")", 86, 84);
        g.drawString(String.valueOf(ctx.skills.level(14)), 90, 103);
        int currentLevel = 0;
        g.drawString(String.valueOf(currentLevel), 86, 123);
        g.drawString(status, 52, 141);
    }
}
