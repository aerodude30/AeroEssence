package aerodude30.com;

import aerodude30.com.controller.Actionable;
import aerodude30.com.controller.JSB;
import aerodude30.com.view.GUI;
import org.powerbot.script.Area;
import org.powerbot.script.Condition;
import org.powerbot.script.PaintListener;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Random;
import org.powerbot.script.Script;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.Bank;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Equipment.Slot;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.Npc;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Created by cbartram on 7/6/2016.
 *
 */
@Script.Manifest(name = "AeroEssence", properties = "author=aerodude30; topic=1296203; client=4;", description = "Mines Rune Essence in east Varrock mine. Supports Bronze-dragon pickaxes.")
public class AeroEssence extends PollingScript<ClientContext> implements PaintListener {

    public Controller controller = ctx.controller;

    private Area VARROCK_BANK  = new Area(new Tile(3250, 3422, 0), new Tile(3257, 3422, 0), new Tile(3257, 3420, 0), new Tile(3250, 3420, 0));
    private Area AUBURY_HUT = new Area(new Tile(3252, 3404, 0), new Tile(3254, 3401, 0), new Tile(3253, 3399, 0), new Tile(3252, 3399, 0));
    private Tile rndTile = AUBURY_HUT.getRandomTile();
    private static final int[] PICKAXE = {1265, 1267, 1269, 1271, 1273, 1275, 15259};
    private final Tile[] pathToAubury = {
            new Tile(3254, 3425, 0),
            new Tile(3258, 3411, 0),
            new Tile(3258, 3405, 0),
            new Tile(3253, 3399, 0),
            new Tile(3255, 3398, 0),
            rndTile
    };

    private static final int RUNE_ESSENCE = 1436, AUBURY_DOOR_CLOSED = 11780, AUBURY_NPC = 637, PORTAL = 7475, RUNE_ESSENCE_ROCK = 7471, BANK_STAIRS = 11793;
    private long startTime;
    private String status = "Waiting to start...";
    private int startExperience = 0;
    private final String startingLevel = String.valueOf(ctx.skills.level(14));

    //Class instances
    private Random rnd = new Random();
    private Util util = new Util();
    private JSB jsb = new JSB();

    //enums for each state in the script
    private enum State {BANK, TRAVERSE, TELEPORT, MINE, REVERSE, STUCK}

    private State getState() {
        //if the player has a pickaxe in their inventory, or equipped, and their backpack does not have Rune ess, and the portal to leave the rune essence area is not in view
        // and there is a distance between the players tile and that of the Aubury's hut area.
        if((ctx.inventory.select().id(PICKAXE).count() == 1 || ctx.equipment.itemAt(Slot.MAIN_HAND).id() == PICKAXE[0]) &&
                ctx.inventory.select().id(RUNE_ESSENCE).count() == 0 &&
                !ctx.objects.select().id(PORTAL).poll().inViewport() && ctx.movement.distance(ctx.players.local().tile(), rndTile) > 3) {
            return State.TRAVERSE;
        }

        if(AUBURY_HUT.contains(ctx.players.local().tile())) {
            return State.TELEPORT;
        }

        if(ctx.game.floor() > 1)  {
            return State.STUCK;
        }

        //if player is in the bank and they have more than one rune essence
        if(VARROCK_BANK.contains(ctx.players.local().tile()) && ctx.inventory.select().id(RUNE_ESSENCE).count() >= 1) {
            return State.BANK;
        }
        //if player has a full inventory of rune essence or a full inventory - 1 because of the pickaxe in their inventory
        return ctx.inventory.select().id(RUNE_ESSENCE).count() == 28 || ctx.inventory.select().id(RUNE_ESSENCE).count() == 27 ? State.REVERSE : State.MINE;
    }

    private void customAction() {
        if(ctx.players.local().inCombat()) {
            //run from mugger
        }
    }

    public void actionBank() {
        //this.state = State.BANK;
    }

    @Override
    public void start() {
        startTime = System.currentTimeMillis();
        startExperience = ctx.skills.experience(14);
        GUI g = new GUI();
        g.pack();
        g.setVisible(true);

        try {
            jsb.initialize("AeroEssence", "1.0", status, g.username.getText(), g.email.getText(), "")
                    .actionListener(new Actionable() {
                        @Override
                        public void customScriptAction() {
                            customAction();
                        }
                    }, "Flee from Mugger")
                    .sendData()
                    .onStop(true);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void poll() {
        State state = getState();
        System.out.println(state);
        util.dismissRandom();

        switch(state) {
            case STUCK:
                status = "Oops! Climbing down";
                ctx.objects.select().id(BANK_STAIRS).nearest().poll().interact("Climb-down", "Staircase");
                break;

            case BANK:
                status = "Banking...";
                if(ctx.bank.inViewport() && !ctx.bank.opened()) {
                    ctx.camera.turnTo(ctx.npcs.select().id(2897, 2898).shuffle().poll());

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
                    ctx.bank.deposit(RUNE_ESSENCE, Bank.Amount.ALL);

                    ctx.bank.close();
                }
                    break;

            case TRAVERSE:
                status = "Walking to Aubury";
                ctx.movement.newTilePath(pathToAubury).traverse();

                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        System.out.println("Player is in motion sleeping...");
                        return !ctx.players.local().inMotion();
                    }
                }, 2500, 10);

                break;

            case TELEPORT:
                final Npc aubury = ctx.npcs.select().id(AUBURY_NPC).poll();
                final GameObject door = ctx.objects.select().id(AUBURY_DOOR_CLOSED).poll();

                status = "Teleporting from Aubury";

                if(!ctx.movement.reachable(ctx.players.local().tile(), new Tile(3253, 3404, 0))) {
                    status = "Opening Door";
                    if(door.orientation() == 0) {
                        ctx.camera.turnTo(door);
                        ctx.objects.select().id(AUBURY_DOOR_CLOSED).nearest().poll().click();
                        aubury.interact("Teleport", aubury.name());
                    }
                } else {
                    aubury.interact("Teleport", aubury.name());
                }
            break;

            case MINE:
                GameObject essence = ctx.objects.select().id(RUNE_ESSENCE_ROCK).poll();
                status = "Mining...";

                if (ctx.players.local().animation() == 625 && ctx.inventory.select().id(RUNE_ESSENCE).count() < 27) {
                    System.out.println("Player is mining, sleeping.");
                    Condition.sleep();
                } else {
                    ctx.movement.step(essence);
                    Condition.sleep(Random.nextInt(2000, 3000));
                    ctx.camera.turnTo(essence);
                    essence.interact("Mine");
                }

                break;

            case REVERSE:
                GameObject portal = ctx.objects.select().name("Portal").poll();
                status = "Returning to Runescape";
                if(portal.inViewport()) {
                    portal.interact("Use", portal.name()); //todo might not be use
                } else {
                    ctx.movement.step(portal);
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return ctx.movement.distance(portal) < 5;
                        }
                    }, 1500, 5);
                    portal.interact("Use", portal.name());
                }
                //if player is in Aubury's hut with a full inventory
                status = "Walking back to bank...";
                if(AUBURY_HUT.contains(ctx.players.local().tile()) && ctx.inventory.count() >= 27) {
                    ctx.movement.newTilePath(pathToAubury).reverse();
                }
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
        g.drawString(util.perHour(expGained, startTime), 86, 84);
        g.drawString(startingLevel, 90, 103);
        g.drawString(String.valueOf(ctx.skills.level(14)), 86, 123);
        g.drawString(status, 52, 141);

    }
}
