package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RuneConfigLoader {
  private static final Logger log = Global.getLogger(RuneConfigLoader.class);
  private static final boolean ENABLE_DETAIL_LOGGING = false;
  public static final String RUNE_CONFIG_PATH = "data/config/rune_sentences.json";
  private static final Map<String, String> runePathCache = new HashMap<String, String>();
  private static final Map<String, SpriteAPI> runeSpriteCache = new HashMap<String, SpriteAPI>();
  private static boolean runePathCacheInitialized = false;
  public static final List<List<String>> runeSentences = new ArrayList<List<String>>();
  private static boolean runeSentencesLoaded = false;

  public static void initializeRunePathCache() {
    if (runePathCacheInitialized) return;
    String[] letters = {
      "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S",
      "T", "U", "W"
    };
    runePathCache.clear();
    runeSpriteCache.clear();
    try {
      for (String letter : letters) {
        String hebrewName = getHebrewNameForLetter(letter);
        String spritePath = "graphics/fx/font/" + letter + "-" + hebrewName + ".png";
        runePathCache.put(letter, spritePath);
        try {
          SpriteAPI sprite = Global.getSettings().getSprite(spritePath);
          if (sprite != null) {
            runeSpriteCache.put(letter, sprite);
          }
        } catch (Exception e) {
          if (ENABLE_DETAIL_LOGGING) {
            log.info("[RuneConfigLoader] Failed to load rune sprite: " + spritePath, e);
          }
        }
      }
      runePathCache.put(" ", null);
      runeSpriteCache.put(" ", null);
      runePathCacheInitialized = true;
    } catch (Exception e) {
      if (ENABLE_DETAIL_LOGGING) {
        log.info("[RuneConfigLoader] Failed to initialize rune path cache", e);
      }
      runePathCacheInitialized = true;
    }
  }

  public static SpriteAPI getRuneSprite(String letter) {
    if (!runePathCacheInitialized) {
      initializeRunePathCache();
      if (ENABLE_DETAIL_LOGGING) {
        log.info("[RuneConfigLoader] Initialization complete; cache size: " + runeSpriteCache.size());
      }
    }
    SpriteAPI sprite = runeSpriteCache.get(letter);
    if (sprite == null && ENABLE_DETAIL_LOGGING && !" ".equals(letter)) {
      log.info("[RuneConfigLoader] Unable to get rune sprite for: " + letter);
    }
    return sprite;
  }

  public static void loadRuneSentences() {
    if (runeSentencesLoaded) return;
    initializeRunePathCache();
    try {
      JSONObject runeConfig = loadConfigFile();
      if (runeConfig == null || !runeConfig.has("sentences")) {
        if (ENABLE_DETAIL_LOGGING) {
          log.info("[RuneConfigLoader] Rune configuration is invalid; creating default sentence");
        }
        createDefaultSentence();
        runeSentencesLoaded = true;
        return;
      }
      JSONArray sentences = runeConfig.getJSONArray("sentences");
      if (sentences.length() == 0) {
        if (ENABLE_DETAIL_LOGGING) {
          log.info("[RuneConfigLoader] Rune configuration is empty; creating default sentence");
        }
        createDefaultSentence();
        runeSentencesLoaded = true;
        return;
      }
      parseSentences(sentences);
      runeSentencesLoaded = true;
      if (runeSentences.isEmpty()) {
        if (ENABLE_DETAIL_LOGGING) {
          log.info("[RuneConfigLoader] No valid rune sentences loaded from configuration; creating default sentence");
        }
        createDefaultSentence();
      }
    } catch (JSONException e) {
      if (ENABLE_DETAIL_LOGGING) {
        log.info("[RuneConfigLoader] JSONException while loading rune sentences", e);
      }
      createDefaultSentence();
      runeSentencesLoaded = true;
    } catch (Exception e) {
      if (ENABLE_DETAIL_LOGGING) {
        log.info("[RuneConfigLoader] Exception while loading rune sentences", e);
      }
      createDefaultSentence();
      runeSentencesLoaded = true;
    }
  }

  private static JSONObject loadConfigFile() {
    try {
      return Global.getSettings().loadJSON(RUNE_CONFIG_PATH);
    } catch (Exception e) {
      if (ENABLE_DETAIL_LOGGING) {
        log.info("[RuneConfigLoader] Failed to load configuration from primary path: " + RUNE_CONFIG_PATH, e);
      }
      try {
        String fullPath = "data/config/rune_sentences.json";
        return Global.getSettings().loadJSON(fullPath);
      } catch (Exception ex) {
        if (ENABLE_DETAIL_LOGGING) {
          log.info("[RuneConfigLoader] Failed to load configuration from fallback path", ex);
        }
      }
    }
    return null;
  }

  private static void parseSentences(JSONArray sentences) throws JSONException {
    for (int i = 0; i < sentences.length(); i++) {
      JSONObject sentence = sentences.getJSONObject(i);
      if (!sentence.has("id")) {
        continue;
      }
      List<String> sentenceLetters = new ArrayList<String>();
      if (sentence.has("letters")) {
        JSONArray lettersArray = sentence.getJSONArray("letters");
        if (lettersArray.length() == 0) {
          continue;
        }
        for (int j = 0; j < lettersArray.length(); j++) {
          sentenceLetters.add(lettersArray.getString(j));
        }
      } else if (sentence.has("text")) {
        String fullText = sentence.getString("text");
        if (fullText == null || fullText.isEmpty()) {
          continue;
        }
        for (char c : fullText.toCharArray()) {
          sentenceLetters.add(String.valueOf(c));
        }
      } else {
        continue;
      }
      if (!sentenceLetters.isEmpty()) {
        runeSentences.add(sentenceLetters);
      }
    }
  }

  private static void createDefaultSentence() {
    runeSentences.clear();
    List<String> defaultSentence = new ArrayList<String>();
    defaultSentence.add("A");
    defaultSentence.add("N");
    defaultSentence.add("Y");
    runeSentences.add(defaultSentence);
  }

  private static String getHebrewNameForLetter(String letter) {
    switch (letter) {
      case "A":
        return "Aleph";
      case "B":
        return "Bet";
      case "C":
        return "Gimel";
      case "D":
        return "Dalet";
      case "E":
        return "He";
      case "F":
        return "Vav";
      case "G":
        return "Zayin";
      case "H":
        return "Het";
      case "I":
        return "Tet";
      case "J":
        return "Yod";
      case "K":
        return "Kaf";
      case "L":
        return "Lamed";
      case "M":
        return "Mem";
      case "N":
        return "Nun";
      case "O":
        return "Samekh";
      case "P":
        return "Ayin";
      case "Q":
        return "Pe";
      case "R":
        return "Tsadi";
      case "S":
        return "Qof";
      case "T":
        return "Resh";
      case "U":
        return "Shin";
      case "W":
        return "Tav";
      default:
        return "Unknown";
    }
  }

  public static boolean isLoaded() {
    return runeSentencesLoaded;
  }

  public static void reset() {
    runeSentencesLoaded = false;
    runePathCacheInitialized = false;
    runeSentences.clear();
    runePathCache.clear();
    runeSpriteCache.clear();
  }
}
