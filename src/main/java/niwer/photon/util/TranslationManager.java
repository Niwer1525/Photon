package niwer.photon.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.sql.PlayerAccountTable;

public class TranslationManager {
	
	/* The list that saves the languages and properties for these languages */
	private static final Map<Language, Map<String, String>> TRANSLATIONS = new EnumMap<>(Language.class);
	
	public static enum Language {
		ENGLISH("en", "US"),
		FRENCH("fr", "FR"),
		GERMAN("de", "DE"),
		RUSSIAN("ru", "RU");

		private final String code;
		private final Locale locale;

		Language(String code, String country) {
			this.code = code;
			this.locale = Locale.of(code, country);
		}

		public String code() { return this.code; }

		public Locale locale() { return this.locale; }

		/**
		 * Get the language from the given name or code.
		 * 
		 * @param name The name or code of the language. (e.g "ENGLISH", "en", "fr", ...)
		 * @return The language if found, otherwise the default language (ENGLISH).
		 */
		public static Language fromString(String name) {
			if(name == null || name.isBlank()) return ENGLISH; /* Default to English if the name is null or blank */

			for (final Language LANG : Language.values()) {
				if (LANG.name().equalsIgnoreCase(name) || LANG.code.equalsIgnoreCase(name)) return LANG;
			}
			
			/* Default to English if the name is not found */
			return ENGLISH;
		}
	}

	/**
	 * Load all languages from the given path.
	 * 
	 * @param path The path to load from.
	 */
	public static void loadAllLanguages(String path) {
		for(final Language LANGUAGE : Language.values()) load(LANGUAGE, path);
	}
	

	/** 
	 * Load a language from the given path.
	 * 
	 * @param locale The locale to load. (e.g locale_en, locale_fr, locale_ru, ...)
	 * @param path The path to load from.
	 * @see TranslationManager#load(String, String)
	 */
	private static void load(Language lang, String path) {
		try(final InputStream STREAM = OperatingSystem.loadFile(path + "/lang_" + lang.code() + ".properties")) {
			try(final BufferedReader READER = new BufferedReader(new InputStreamReader(STREAM, "UTF-8"))) {
				/* Create a list to save the properties (keys and value) for this language */
				final Map<String, String> PROPERTIES = new HashMap<String, String>();

				String line;
				while((line = READER.readLine()) !=null) {
					if (line.startsWith("#") || line.startsWith("//") || line.trim().isEmpty()) continue; /* If the line is a comment or empty, ignore it */

					final int EQUALS_INDEX = line.indexOf("=");
					if (EQUALS_INDEX == -1) continue; /* Skip lines without '=' */

					PROPERTIES.put(
						line.substring(0, EQUALS_INDEX), // Get the line from 0 to the first '='
						line.substring(EQUALS_INDEX + 1) // Get the line from the first '=' to the end
					);
				}
				
				/* Added the properties to then required language */
				TRANSLATIONS.put(lang, PROPERTIES);
			}
		}
		catch (IOException | NullPointerException e) {
			Console.log("Error loading language file for " + lang.code() + ": " + e.getMessage()).type(PhotonLogTypes.NETWORK).error().container(PhotonEngine.LOGGER).send();
		}
	}

	private static Map<String, String> getProperties(Language lang) {
		Map<String, String> properties = TRANSLATIONS.get(lang);
		if (properties == null) properties = TRANSLATIONS.getOrDefault(Language.ENGLISH, new HashMap<>()); // If the language is not found, return the properties for English (as it's the default language)
		return properties;
	}

	/**
	 * Get the translation for the given key.
	 * 
	 * @param lang The language to get the translation for. (e.g en, fr, ru, ...)
	 * @param key The key to get the translation for. (e.g "button.title")
	 * @param obj The objects to format the translation with. (The system replace every %s with the given object in order)
	 * @return The translation for the given key.
	 */
	public static String format(Language lang, String key, Object... obj) {
		final String TRANSLATION = getProperties(lang).get(key); // Get the propertie for the current key
		return TRANSLATION == null ? key : String.format(TRANSLATION, obj); // Return the translated key if exist, else return the key (to avoid null pointer exception) and format it with the given objects
	}

	/**
	 * Get the translation for the given key for a user.
	 * 
	 * @param discordUserID The discord user id to get the language of the user.
	 * @param key The key to get the translation for. (e.g "button.title")
	 * @param obj The objects to format the translation with. (The system replace every %s with the given object in order)
	 * @return The translation for the given key.
	 */
	public static String format(String discordUserID, String key, Object... obj) {
		return format(PlayerAccountTable.getLanguage(discordUserID), key, obj);
	}
}
