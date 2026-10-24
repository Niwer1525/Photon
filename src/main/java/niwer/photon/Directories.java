package niwer.photon;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import niwer.lumen.Console;
import niwer.photon.util.GsonUtils;
import niwer.photon.util.OperatingSystem;
import niwer.photon.util.PhotonLogTypes;

public class Directories
{
	public static final File BASE_DIR = new File("./network/");
	public static final File LOGS_DIR = new File(BASE_DIR + "/logs/");
	public static final File BACKUPS_DIR = new File(BASE_DIR + "/backups/");
	public static final File LOGO_FILE = new File(BASE_DIR + "/project_logo.png");
	public static final File DATA_BASE_FILE = new File(BASE_DIR, "network.db");

	private static PhotonConfig config = PhotonConfig.DEFAULT;
	public static File configFile = new File(BASE_DIR + "/config.json");
	
	public static PhotonConfig config() { return config == null ? PhotonConfig.DEFAULT : config; }

	private Directories() {}

    /**
     * Load all directories and files
     */
	public static void load() {
		if (!BASE_DIR.exists()) BASE_DIR.mkdirs();
		if (!LOGS_DIR.exists()) LOGS_DIR.mkdirs();
		
		try {
			if (!configFile.exists()) {
				configFile.createNewFile();
				try (var WRITER = new FileWriter(configFile)) { WRITER.write(GsonUtils.PRETTY_GSON.toJson(PhotonConfig.DEFAULT)); }
			}

			/* Read the file */
			final BufferedReader READER = new BufferedReader(new FileReader(configFile));
			config = GsonUtils.PRETTY_GSON.fromJson(READER, PhotonConfig.class);
			READER.close();

			if (config == null) config = new PhotonConfig();
		} catch (IOException e) {}

		save(); // Save the config file to ensure it is up-to-date
	}

	/**
	 * Save all directories and files
	 * 
	 * @note He save the config file
	 * @see {@link #configFile}
	 * @see {@link #config}
	 */
	public static void save() {
		try (var WRITER = new FileWriter(configFile)) {
			config();
			WRITER.write(GsonUtils.PRETTY_GSON.toJson(config));
		} catch (IOException e) {}
	}
	
	/**
	 * Get the official logo as a Base64 string
	 * 
	 * @return The official logo as a Base64 string, or null if it cannot be loaded
	 */
	public static String getOfficialLogoBase64() {
		try {
			if(!LOGO_FILE.exists()) { // If there is no logo, then try to "download" the official one from the jar
				try(final InputStream STREAM = OperatingSystem.loadFile("photon_logo.png"); final FileOutputStream FOS = new FileOutputStream(LOGO_FILE)) {
					final BufferedImage IMAGE = ImageIO.read(STREAM); // Read from stream
					ImageIO.write(IMAGE, "png", FOS); // Write to file
				}
			}

			/* Try to read the official logo file */
			final BufferedImage IMAGE = ImageIO.read(LOGO_FILE);
			if (IMAGE == null) {
				Console.log("Failed to read game logo").error().type(PhotonLogTypes.NETWORK).container(PhotonEngine.LOGGER).send();
				return null;
			}
			
			/* Convert the image to a byte array */
			final ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
			ImageIO.write(IMAGE, "png", BAOS);
			BAOS.close();
			
			return Base64.getEncoder().encodeToString(BAOS.toByteArray()); // Encode the byte array to a Base64 string
		} catch (IOException e) {
			Console.log("Error loading game logo: " + e.getMessage()).error().type(PhotonLogTypes.NETWORK).container(PhotonEngine.LOGGER).send();
			e.printStackTrace();
			return null;
		}	
	}
}