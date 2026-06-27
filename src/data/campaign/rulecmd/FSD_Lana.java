package data.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import java.util.List;
import java.util.Map;

public class FSD_Lana extends BaseCommandPlugin {
  public static final String PERSON_MEM_KEY = "$FSD_Lana";

  @Override
  public boolean execute(
      String ruleId,
      InteractionDialogAPI dialog,
      List<Misc.Token> params,
      Map<String, MemoryAPI> memoryMap) {
    return false;
  }

  public static PersonAPI createPerson(String factionId, String portraitPath, FullName fullName) {
    PersonAPI person;
    if (factionId == null) {
      factionId = "Farsight_Drive";
    }

    if (fullName != null) {
      person = Global.getSector().getFaction(factionId).createRandomPerson(fullName.getGender());
      person.setName(fullName);
    } else {
      person = Global.getSector().getFaction(factionId).createRandomPerson();
    }

    if (portraitPath != null) {
      person.setPortraitSprite(portraitPath);
    }
    person.setPostId(Ranks.UNKNOWN);
    person.setRankId(Ranks.UNKNOWN);
    person.setImportance(PersonImportance.LOW);
    person.getMemoryWithoutUpdate().set(PERSON_MEM_KEY, true);
    return person;
  }
}
