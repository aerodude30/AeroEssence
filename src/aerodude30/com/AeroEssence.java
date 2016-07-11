package aerodude30.com;

import org.powerbot.script.Area;
import org.powerbot.script.Condition;
import org.powerbot.script.PaintListener;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Random;
import org.powerbot.script.Script;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Equipment.Slot;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.Interactive;
import org.powerbot.script.rt4.Npc;

import java.awt.*;
import java.util.concurrent.Callable;


/**
 * Created by cbartram on 7/6/2016.
 *
 */
@Script.Manifest(name = "AeroEssence", properties = "author=aerodude30; topic=1296203; client=4;", description = "Intelligent East Varrock Rune Essence Miner.")
public class AeroEssence extends PollingScript<ClientContext> implements PaintListener {

    //Class instances
    private Random rnd = new Random();
    private Util util = new Util();

    private Tile rndTile = AUBURY_HUT.getRandomTile();
    private final Tile[] pathToAubury = {
            new Tile(3254, 3425, 0),
            new Tile(3258, 3411, 0),
            new Tile(3258, 3405, 0),
            new Tile(3253, 3399, 0),
            new Tile(3255, 3398, 0),
            rndTile
    };
    private final Tile[] pathToBank =  {
        new Tile(3258,3405, 0),
        new Tile(3262, 3417, 0),
        new Tile(3255, 3429, 0),
        VARROCK_BANK.getRandomTile()
    };

    //Variables
    private int essenceMined = 0;
    private long startTime;
    private String status = "Waiting to start...";
    private int startExperience = 0;

    //Constants
    private final String START_LEVEL = String.valueOf(ctx.skills.level(14));
    private static final int RUNE_ESSENCE = 1436, AUBURY_DOOR_CLOSED = 11780, AUBURY_NPC = 637, RUNE_ESSENCE_ROCK = 7471, BANK_STAIRS = 11793, STRAY_DOG= 2902;
    private static final int[] PORTAL =  {7472, 7473, 7475, 7477, 7479, 7474, 7476, 7480};
    private static final int[] PICKAXE = {1265, 1267, 1269, 1271, 1273, 1275, 15259};
    private static Area VARROCK_BANK  = new Area(new Tile(3250, 3422, 0), new Tile(3257, 3422, 0), new Tile(3257, 3420, 0), new Tile(3250, 3420, 0));
    private static Area AUBURY_HUT = new Area(new Tile(3252, 3404, 0), new Tile(3254, 3401, 0), new Tile(3253, 3399, 0), new Tile(3252, 3399, 0));
    private static final int[] DOOR_BOUNDS = {12, 136, -228, -4, -20, 48};

    //Enumerations for each State in the script
    private enum State {BANK, TRAVERSE, TELEPORT, MINE, REVERSE, STUCK, STRAY_DOG}

    /**
     *Returns the current state of the script
     * @return State Returns a state object for the currently active state in the script
     */
    private State getState() {

        if((ctx.inventory.select().id(PICKAXE).count() == 1 || contains(PICKAXE, ctx.equipment.itemAt(Slot.MAIN_HAND).id())) &&
                ctx.inventory.select().id(RUNE_ESSENCE).count() == 0 &&
                !ctx.objects.select().id(PORTAL).poll().inViewport() && ctx.movement.distance(ctx.players.local().tile(), rndTile) >= 3
                && !ctx.npcs.select().id(STRAY_DOG).nearest().poll().inViewport()) {
            return State.TRAVERSE;
        }

        if(AUBURY_HUT.contains(ctx.players.local().tile()) && ctx.inventory.select().id(RUNE_ESSENCE).count() == 0) { return State.TELEPORT; }

        if(ctx.players.local().tile().floor() >= 1)  { return State.STUCK; }

        if(ctx.movement.distance(ctx.players.local(), ctx.npcs.select().id(STRAY_DOG).poll()) <= 2 && ctx.npcs.select().id(STRAY_DOG).poll().inViewport()) { return State.STRAY_DOG; }

        if(VARROCK_BANK.contains(ctx.players.local().tile()) && ctx.inventory.select().id(RUNE_ESSENCE).count() >= 1) { return State.BANK; }

        return ctx.inventory.select().id(RUNE_ESSENCE).count() == 28 || ctx.inventory.select().id(RUNE_ESSENCE).count() == 27 ? State.REVERSE : State.MINE;
    }

    /**
     * Iterates through an array to see if a specific value is contained in the array
     * @param arr Array to iterate through
     * @param value value to search for
     * @return true if the value was found otherwise false
     */
    private boolean contains(int[] arr, int value) {
        for(int anArr : arr) {
            if(anArr == value) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void start() {
        startTime = System.currentTimeMillis();
        startExperience = ctx.skills.experience(14);
    }

    @Override
    public void poll() {
        State state = getState();
        util.dismissRandom();
        GameObject hutDoor = ctx.objects.select().id(AUBURY_DOOR_CLOSED).each(Interactive.doSetBounds(DOOR_BOUNDS)).nearest().poll();

        switch(state) {
            case STUCK:
                //if there was a misclick and the player ended up on the second floor of the bank.
                status = "Oops! Climbing down";
                ctx.objects.select().id(BANK_STAIRS).nearest().poll().interact("Climb-down", "Staircase");
                Condition.sleep(Random.nextInt(500, 1000));
                break;

            case STRAY_DOG:
                //shoo's away stray dog which could potentially block the player in a location.
                status = "Shooing stray dog";
                final Npc strayDog = ctx.npcs.select().id(STRAY_DOG).nearest().poll();
                    strayDog.interact("Shoo-away", strayDog.name());
                    Condition.wait(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return ctx.movement.distance(ctx.players.local().tile(), strayDog.tile()) >= 4;
                        }
                    },700, 3);
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
                } else {
                    status = "Depositing Essence...";

                    ctx.bank.depositInventory();

                    ctx.bank.close();
                    essenceMined += 27;
                }

                    break;

            case TRAVERSE:
                status = "Walking to Aubury";
                ctx.movement.newTilePath(pathToAubury).traverse();

                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return !ctx.players.local().inMotion();
                    }
                }, 2500, 10);

                break;

            case TELEPORT:
                final Npc aubury = ctx.npcs.select().id(AUBURY_NPC).poll();
                status = "Teleporting from Aubury";

                //Player is stuck inside or outside Aubury's hut
                if(!ctx.movement.reachable(ctx.players.local().tile(), new Tile(3253, 3404, 0))) {
                    status = "Opening Door";
                    final GameObject closedDoor = ctx.objects.select().id(AUBURY_DOOR_CLOSED).poll();
                    if(closedDoor.valid()) {
                        ctx.camera.turnTo(hutDoor);
                        closedDoor.click();
                        aubury.interact("Teleport", aubury.name());
                    }
                } else {
                    aubury.interact("Teleport", aubury.name());
                    Condition.sleep(Random.nextInt(2000, 2500));
                }
            break;

            case MINE:
                GameObject essence = ctx.objects.select().id(RUNE_ESSENCE_ROCK).nearest().poll();
                status = "Mining...";

                //While the player is mining, run the anti-pattern and sleep
                if (ctx.players.local().animation() == 625 && ctx.inventory.select().id(RUNE_ESSENCE).count() < 27) {
                    util.antiPattern();
                    Condition.sleep();
                } else {
                    ctx.movement.step(essence);
                    Condition.sleep(Random.nextInt(2000, 3000));
                    ctx.camera.pitch(Random.nextInt(0, 50));
                    essence.interact("Mine");
                }

                break;

            case REVERSE:
                GameObject portal = ctx.objects.select().id(PORTAL).nearest().poll();
                status = "Returning to Runescape";

                if(!ctx.movement.reachable(ctx.players.local().tile(), new Tile(3253, 3398, 0))) {
                status = "Door is closed, opening...";

                if(hutDoor.valid() && !hutDoor.inViewport()) {
                    ctx.camera.turnTo(hutDoor);
                    hutDoor.click(true);
                    }
                }

                if(ctx.inventory.select().id(RUNE_ESSENCE).count() >= 27 && ctx.movement.distance(ctx.players.local().tile(), VARROCK_BANK.getRandomTile()) > 3) {
                    status = "Walking back to bank...";
                    ctx.movement.newTilePath(pathToBank).traverse();
                    while (ctx.players.local().inMotion()) {
                        Condition.sleep();
                    }

                } else {

                    if(!AUBURY_HUT.contains(ctx.players.local().tile()) && ctx.movement.distance(ctx.players.local().tile(), ctx.objects.select().id(RUNE_ESSENCE_ROCK).poll()) <= 5) {
                        ctx.camera.turnTo(portal);
                        ctx.movement.step(portal);
                        portal.click(true);
                    }

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
        g.fillRect(3, 3, 195, 185);
        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(51, 153, 255));
        g.drawRect(3, 3, 195, 185);
        g.setColor(new Color(51, 153, 255));
        g.drawLine(12, 31, 187, 31);
        g.setColor(new Color(255, 255, 255));
        g.setFont(new Font("Arial", 0, 14));
        g.drawString("AeroEssence", 61, 25);
        g.setColor(new Color(255, 255, 255));
        g.setFont(new Font("Arial", 0, 11));
        g.drawString("Time Running: " , 13, 48);
        g.drawString("Mining Exp Gained: ", 14, 65);
        g.drawString("Mining/hour: ", 14, 84);
        g.drawString("Starting Level: ", 15, 103);
        g.drawString("Current Level: ", 15, 123);
        g.drawString("Status: ", 16, 141);
        g.drawString("Essence Mined: ", 16, 159);
        g.drawString("Profit: ", 16, 178);
        g.drawString(util.runtime(startTime), 85, 49);
        g.drawString(String.valueOf(expGained), 110, 65);//
        g.drawString(util.perHour(expGained, startTime), 76, 84);//
        g.drawString(START_LEVEL, 87, 103);
        g.drawString(String.valueOf(ctx.skills.level(14)), 86, 123);
        g.drawString(status, 52, 141);
        g.drawString(String.valueOf(essenceMined), 96, 159);
        g.drawString(String.valueOf(util.formatNumber(util.getPrice(RUNE_ESSENCE) * essenceMined)), 48, 178);

    }
}
