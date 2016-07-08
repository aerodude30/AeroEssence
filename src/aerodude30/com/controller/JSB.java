package aerodude30.com.controller;

import aerodude30.com.AeroEssence;
import aerodude30.com.view.GUI;
import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSB - Java Server Bridge
 * Created by Christian Bartram on March 27th, 2016
 * @version 1.2.0
 */

public class JSB implements aerodude30.com.controller.Actionable {

    private int                     secondsCounter = 0;
    private ConcurrentHashMap<String, Serializable> queueInfo = new ConcurrentHashMap<>();
    private static final String[]   skills = new String[]{"Attack", "Strength", "Defense", "Magic", "Ranged", "Prayer", "Runecrafting", "Dungeoneering", "Construction",
            "Constitution", "Agility", "Herblore", "Thieving", "Crafting", "Fletching", "Slayer", "Hunter", "Mining", "Smithing",
            "Fishing", "Cooking", "Firemaking", "Woodcutting", "Farming", "Summoning"};
    private static final char[]     KEY = "enfldsgbnlsngdlksdsgm".toCharArray();
    private static final String     META = "ZdTvXr7WwyxORVdvZjS7xV8WpLhxLD3RnZb5FVpBKYN9jqi3kFhDQA==";
    private static final byte[]     SALT = {(byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12, (byte) 0xde, (byte) 0x33, (byte) 0x10, (byte) 0x12,};
    private final ArrayList<String> url = new ArrayList<>();
    private boolean                 shutdown = false;
    private AeroEssence aeroEssence = new AeroEssence();

    /**
     * Reads encrypted data into the program from an external source
     */
    public void populateDataList() {
        try {
            URL url = new URL(decrypt(META));
            URLConnection con = url.openConnection();
            con.setRequestProperty("Connection", "close");
            con.setDoInput(true);
            con.setUseCaches(false);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String line;
            int i = 0;
            while((line = in.readLine()) != null) {
                this.url.add(i, line);
                i++;
            }
        }   catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * The active scripts primary banking method.
     * This method is called from the actionListener() method. When a user clicks on
     * the "Bank" button in the web app's dashboard this method will be executed.
     * This method should properly bank according to what the developer's script is performing
     * and the current location the player is in.
     */
    public void bank() {
        System.out.println("Walking to bank....Banking items...resuming script...");
    }

    /**
     * This method defines the code to be executed when the custom script action button is clicked from
     * the Aeroscripts web application.
     */
    public void customScriptAction() {
        System.out.println("Custom Action button clicked!");
    }

    /**
     * Decrypts an encrypted string
     * @param property The string to decrypt
     * @return A decrypted version of the encrypted input string
     * @throws GeneralSecurityException
     * @throws IOException
     */
    private String decrypt(String property) throws GeneralSecurityException, IOException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(new PBEKeySpec(KEY));
        Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
        pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
        return new String(pbeCipher.doFinal(base64Decode(property)), "UTF-8");
    }

    /**
     * Decodes a Base64 encoded String into a newly-allocated byte array using the Base64 encoding scheme.
     * @param property Encoded String input
     * @return New Base64 byte array
     * @throws IOException
     */
    private static byte[] base64Decode(String property) throws IOException {return new BASE64Decoder().decodeBuffer(property);}


    /**
     * Sets the starting data to send to the web engine on script start and initializes all objects and instances
     * This method also creates a unique sessionID to identify the script instance on the website. Initialize() removes any previous data
     * on the users account from previously running scripts ensuring a clean slate for each scripting instance. <b>Important: This method must be called first in the method chain
     * to ensure all objects and instances are properly created and initialized!</b>
     * Initialize() passes given values into a HashMap to be refreshed as set intervals and sent to the web engine for translation into analytical data

     * @param scriptName The script name to be displayed
     * @param version  The scripts version e.g (1.2.30)
     * @param status The status of the currently running script
     * @param username The Aeroscripts username of the user currently running the RSBot script
     * @param email The Aeroscripts email of the user currently running the RSBot script
     * @param API_KEY The Developer's Unique API Key used to verify the session as a legitimate developer account
     * @return JSB object for optional method chaining.
     */
    public JSB initialize(String scriptName, final String version, String status, final String username, final String email, final String API_KEY) {

        if(new GUI().optedOut) {
            onStop(false);
        }
        if (notNull(scriptName, version, status, username, email)) {

            String sessionID = generateSessionID();

            try {
                scriptName = URLEncoder.encode(scriptName, "UTF-8");
                status = URLEncoder.encode(status, "UTF-8");
            } catch(IOException e) {
                System.err.println("[ERROR] Could not encode URL with arguments...some data may not be sent correctly");
            }

            populateDataList();
            deletePreviousData();

            System.out.println("[INFO] Initializing Aeroscripts");
            System.out.println("[INFO] Script started with SessionID: " + sessionID);

            queueInfo.put("ScriptName", scriptName);
            queueInfo.put("API_KEY", API_KEY);
            queueInfo.put("Version", version);
            queueInfo.put("Username", username);
            queueInfo.put("Email", email);
            queueInfo.put("Status", status);
            queueInfo.put("ID", sessionID);
            queueInfo.put("Runtime", 0);
            queueInfo.put("Image", "screenshot-" + sessionID + ".jpg");


            for(String skill : skills) {
                setSkillExperience(skill, 0);
            }


        } else {
            throw new NullPointerException("One of the method arguments is null");
        }
        return this;
    }

    /**
     * Checks arguments in a method for null values
     * @param args the values to check
     * @return true if there are no null values otherwise false if null values were found
     */
    private boolean notNull(Object... args) {
        for (Object arg : args) {
            if (arg == null) { return false; }
        }
        return true;
    }

    /**
     * Sets the current status of the running script
     * @param status the status to set for the script
     * @return JSB object for optional method chaining.
     */
    public JSB setStatus(String status) {
        try {
            status = URLEncoder.encode(status, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            System.err.println("Status could not be encoded to URL safe status...some data might not be sent correctly");
        }
        queueInfo.put("Status", status); return this;
    }

    /**
     * Retrieves the status of the currently running script
     * @return A String representing the current status of the script
     */
    public String getStatus() {
        return queueInfo.get("Status").toString();
    }

    /**
     * Sets the current experience in any particular skill running in the script
     * @param skill the skill to set
     * @param experience the amount of experience earned in the particular skill
     * @return JSB object for optional method chaining.
     */
    public JSB setSkillExperience(String skill, int experience) {
        queueInfo.put(skill, experience);
        return this;
    }

    /**
     * Sets an array of skills and their corresponding array of experience for each skill to send to the web engine.
     * Each skill starts with a capital letter and all remaining letters are lowercase.
     * @param skills A String array of skills to send. String[] skills = new String[] {"Attack", "Defense", "Strength" }
     * @param experience An Integer Array of experience that directly corresponds to each element in the skills array in the example above :
     *                   int[] experienceArray = new int[] {123, 456, 789} would correspond to Attack = 123, Defense = 456, Strength = 789
     * @return JSB object for method chaining.
     */
    public JSB setSkillExperience(String[] skills, int[] experience) {
        if(skills.length != experience.length) {
            System.err.println("[Error] Each skill must correspond with each element in experience array. Skill length: " + skills.length + " Experience Length: " + experience.length);
        } else {
            for (int i = 0; i < skills.length; i++) {
                queueInfo.put(skills[i], experience[i]);
            }
        }
        return this;
    }

    /**
     * Gets the experience set in a skill
     * @param skill The skill with which to return the experience for
     * @return The current experience earned for any particular skill
     */
    public int getSkillExperience(String skill) {
        return Integer.parseInt(queueInfo.get(skill).toString());
    }

    /**
     * Generates a unique session ID for each instance of the script
     * @return a unique string of characters that represents a unique identification string for each instance of a script
     */
    private String generateSessionID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().substring(0, 8);
    }

    /**
     * Takes a screenshot of the desktop and stores it in RSBot's specified storage directory
     * This method will need to be edited by the developer to call their primary class name instead of Example()
     * @return Returns an image file object (jpg) representing the screenshot of the currently running script
     * @throws AWTException
     * @throws IOException
     */
    public File screenshot() throws AWTException, IOException {
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage capture = new Robot().createScreenCapture(screenRect);
        File f  = new File(aeroEssence.controller.script().getStorageDirectory().getAbsolutePath() + "/" + queueInfo.get("Image"));
        ImageIO.write(capture, "jpg", f);
        f.deleteOnExit();
        return f;
    }


    /**
     * Listens to actions for the script to execute when the user interacts with the control panel in the web application
     * This method will need to be edited by the developer to call their primary class name instead of Example()
     * @param action Actionable interface instance.
     * @param buttonTitle Title to be shown on the custom script action button in the web app.
     * @throws IOException An IOException is thrown if the URL cannot be reached or is invalid
     * @return JSB object for optional method chaining
     */
    public JSB actionListener(Actionable action, String buttonTitle) throws IOException {
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    URL data = new URL(decrypt(url.get(0)) +
                            queueInfo.get("Username") +
                            "&Email=" + queueInfo.get("Email") +
                            "&ButtonTitle=" + URLEncoder.encode(buttonTitle, "UTF-8") +
                            "&ID=" + queueInfo.get("ID"));
                    URLConnection con = data.openConnection();
                    con.setRequestProperty("Connection", "close");
                    con.setDoInput(true);
                    con.setUseCaches(false);
                    con.setDoOutput(true);

                    String line;
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                        switch (line) {
                            case "resume":
                                System.err.println("[ACTION] Resume Script");
                                aeroEssence.controller.resume();
                                break;
                            case "shutdown":
                                System.err.println("[ACTION] Emergency Shutdown...");
                                shutdown = true;
                                onStop(false);
                                aeroEssence.controller.stop();
                                break;
                            case "pause":
                                System.err.println("[ACTION] Pause Script");
                                aeroEssence.controller.suspend();
                                break;
                            case "bank":
                                System.err.println("[ACTION] Bank Items");
                                aeroEssence.actionBank();
                                break;
                            case "custom":
                                System.err.println("[ACTION] Custom Script Action");
                                action.customScriptAction();
                                break;
                            default:
                                //do nothing no action has been selected
                                break;
                        }
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 3000);     //checks every 3 seconds
        return this;
    }

    /**
     * Deletes the previous scripts data on script start in case something didn't shut down correctly.
     * Connects to the server and executes an SQL query to delete the script data
     */
    private void deletePreviousData() {
        try {
            URL data = new URL(decrypt(this.url.get(1)) + queueInfo.get("Username") + "&Email=" + queueInfo.get("Email"));
            URLConnection con = data.openConnection();
            con.setRequestProperty("Connection", "close");
            con.setDoInput(true);
            con.setUseCaches(false);
            con.setDoOutput(true);
            String line;
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            while((line = in.readLine()) != null) {
                System.err.println(line);
            }

            System.out.println("[INFO] Deleted outdated script data....");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Clears the data in the dashboard on the web application
     * if the active script has been shutdown by the user or crashes
     * <b>Even though this halts all connections to the web application this should be called from the start() method overridden
     * from PollingScript and chained to the end of the sendData() method </b> see <a href="http://aeroscripts.com/example.php">this page for examples</a>
     * onStop() uses a shutdown hook to make sure it is the last method to execute before the program quits.
     * @param useShutdownHook Boolean value of whether or not to use a shutdown hook (code that executes directly before the program stops)
     */
    public final void onStop(boolean useShutdownHook) {
        if(useShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    URL data = new URL(decrypt(this.url.get(2))
                            + queueInfo.get("Username")
                            + "&Email=" + queueInfo.get("Email")
                            + "&Key=" + queueInfo.get("ID")
                            + "&API_KEY=" + queueInfo.get("API_KEY")
                            + "&ScriptName=" + queueInfo.get("ScriptName"));
                    URLConnection con = data.openConnection();
                    con.setRequestProperty("Connection", "close");
                    con.setDoInput(true);
                    con.setUseCaches(false);
                    con.setDoOutput(true);

                    System.out.println("[INFO] Shutting down....");

                    BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.err.println(line);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "Shutdown-thread"));
        } else {
            //use shutdown hook is false
            try {
                URL data = new URL(this.decrypt(url.get(2))
                        + queueInfo.get("Username")
                        + "&Email=" + queueInfo.get("Email")
                        + "&Key=" + queueInfo.get("ID")
                        + "&API_KEY=" + queueInfo.get("API_KEY")
                        + "&ScriptName=" + queueInfo.get("ScriptName"));
                URLConnection con = data.openConnection();
                con.setRequestProperty("Connection", "close");
                con.setDoInput(true);
                con.setUseCaches(false);
                con.setDoOutput(true);

                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    System.err.println(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Private method used to quickly build a lengthy URL. Used to send all data over to the web application.
     * @param skills String Array of Skills to send.
     * @return The concatenated URL as one String object.
     */
    private String buildURL(String[] skills) {
        try {
            String url = decrypt(this.url.get(3)) + queueInfo.get("ID") + "&Username=" + queueInfo.get("Username") + "&ScriptName=" + queueInfo.get("ScriptName")
                    + "&Email=" + queueInfo.get("Email")
                    + "&Runtime=" + queueInfo.get("Runtime")
                    + "&Version=" + queueInfo.get("Version")
                    + "&Status=" + queueInfo.get("Status")
                    + "&Image=" + queueInfo.get("Image");

            for (String skill : skills) {
                url = url + "&" + skill + "=" + queueInfo.get(skill);
            }
            return url;
        } catch (Exception e) {
            System.err.println("[Error] Could not Build URL...");
            e.printStackTrace();
            return "";
        }

    }

    /**
     * Opens a URL connection to the Aeroscripts server and sends the standard required data for Aeroscripts to function
     * Sends sessionID, Username, Scriptname, Email, Runtime, Version, Status, and a link to the screenshot initialized in the class constructor
     * @throws IOException the user input is incorrect and therefore cannot be validated with the web application
     * @return JSB object for optional method chaining.
     */
    public final JSB sendData() throws  IOException  {
        System.out.println("[INFO] Connecting to Server...");
        System.out.println("[INFO] Connected to Server, Sending Data...");

        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                secondsCounter += 5;
                queueInfo.put("Runtime", secondsCounter);
                try {
                    uploadScreenshot(new FileInputStream(screenshot()));
                    URL data = new URL(buildURL(skills));
                    URLConnection con = data.openConnection();
                    con.setRequestProperty("Connection", "close");
                    con.setDoInput(true);
                    con.setUseCaches(false);
                    con.setDoOutput(true);

                    String line;
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

                    while ((line = in.readLine()) != null) {
                        System.err.println(line);
                        if (line.equalsIgnoreCase("shutdown") || line.contains("shutdown")) {
                            throw new IOException("Input from User could not being validated with server");
                        }
                    }
                } catch (Exception e) {
                    System.err.print("URL Reference cannot be serialized");
                    e.printStackTrace();
                }

                if(shutdown) {
                    cancel();
                    onStop(false);
                }
            }

        }, 0 , 5000);

        return this;
    }

    /**
     * Uploads multipart form data (image) taken from hosts computer to the web application via a HTTP POST request
     * @param stream A File input stream to find the file.
     */
    private void uploadScreenshot(FileInputStream stream) {

        String name = "screenshot-" + queueInfo.get("ID") + ".jpg";

        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";

        try {
            HttpURLConnection conn = (HttpURLConnection)  new URL(decrypt(this.url.get(4)) + queueInfo.get("API_KEY")).openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + name + "\"" + lineEnd);
            dos.writeBytes(lineEnd);


            int bytesAvailable = stream.available();
            int maxBufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            byte[] buffer = new byte[bufferSize];


            int bytesRead = stream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = stream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = stream.read(buffer, 0, bufferSize);
            }

            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            stream.close();
            dos.flush();

            dos.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            new AeroEssence().controller.stop();

        }
    }
}

