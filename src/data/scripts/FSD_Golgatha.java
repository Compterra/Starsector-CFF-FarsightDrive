package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import data.campaign.rulecmd.FSD_Lana;
import org.magiclib.util.MagicCampaign;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import static data.scripts.Farsight_Drive_NormalGenerate.addMarketplace;


public class FSD_Golgatha {

    public void generate(SectorAPI sector) {

        StarSystemAPI system = sector.createStarSystem("Golgatha");
        system.getLocation().set( 4840,-27647);
        system.setBackgroundTextureFilename("graphics/backgrounds/background_galatia.jpg");
        system.setLightColor(new Color(191, 211, 255));
        PlanetAPI Golgatha_star = system.initStar("Golgatha", "star_browndwarf", 800f, 525f, 10f, 0.5f, 2f);
        Golgatha_star.setCustomDescriptionId("Golgatha_star");


        system.addAsteroidBelt(Golgatha_star/* centered on the star, generates a planetary ring */, 500, 2500, 200, 150, 300, Terrain.ASTEROID_BELT,"Crimson Asteroid Belt");
        system.addRingBand(Golgatha_star, "misc", "rings_dust0", 256f, 3, Color.gray, 256f, 2500, 305f, Terrain.RING, "Crimson Dust Ring");
        system.addAsteroidBelt(Golgatha_star, 600, 5720, 250, 100, 280, Terrain.ASTEROID_BELT, "Bright Orange Asteroid Belt");
        system.addRingBand(Golgatha_star, "misc", "rings_asteroids0", 256f, 3, Color.white, 256f, 5720, 295f, Terrain.RING, "Bright Orange Dust Ring");

        SectorEntityToken relay = system.addCustomEntity("FSD_Relay",
                "Crystal Communications Node",
                "comm_relay",
                "Farsight_Drive");
        relay.setCircularOrbitPointingDown(system.getEntityById("Golgatha"),
                245-60,
                4600,
                300);

        SectorEntityToken nav = system.addCustomEntity("FSD_Nav",
                "Reflecting-Light Navigation Node",
                "nav_buoy",
                "Farsight_Drive");
        nav.setCircularOrbitPointingDown(system.getEntityById("Golgatha"),
                600,
                5000,
                150);

        SectorEntityToken sensor = system.addCustomEntity("FSD_Sensor",
                "Wide-Area Sensor Relay",
                "sensor_array",
                "Farsight_Drive");
        sensor.setCircularOrbitPointingDown(system.getEntityById("Golgatha"),
                100,
                7000,
                500);


        PlanetAPI Golgatha_I = system.addPlanet("Golgatha_I",
                Golgatha_star,
                "Matthew",
                "jungle",
                300,
                175,
                5000,
                200);


        Golgatha_I.getSpec().setGlowColor(new Color(190, 154, 252));
        Golgatha_I.getSpec().setUseReverseLightForGlow(true);
        Golgatha_I.setFaction("Farsight_Drive");
        Misc.initConditionMarket(Golgatha_I);


        Golgatha_I.setCustomDescriptionId("Golgatha_I");


        MarketAPI Golgatha_IMarket = addMarketplace(
                "Farsight_Drive",
                Golgatha_I,
                null,
                Golgatha_I.getName(),
               7,
               new ArrayList<>(
                       Arrays.asList(
                                Conditions.POPULATION_7,
                                Conditions.FARMLAND_BOUNTIFUL,
                                Conditions.HABITABLE,
                                Conditions.MILD_CLIMATE,
                                Conditions.ORE_SPARSE


                       )),
                new ArrayList<>(
                        Arrays.asList(
                               Submarkets.SUBMARKET_OPEN,
                                Submarkets.SUBMARKET_STORAGE,
                                Submarkets.GENERIC_MILITARY
                        )),
                new ArrayList<>(
                        Arrays.asList(
                                Industries.POPULATION,
                                Industries.REFINING,
                                Industries.WAYSTATION,
                                Industries.MINING,
                                Industries.HIGHCOMMAND,
                                Industries.GROUNDDEFENSES,
                                Industries.STARFORTRESS_HIGH

                        )
                ),
                0.2f,
                false,
                false
        );
        Golgatha_IMarket.addIndustry("megaport", new ArrayList<String>(Arrays.asList("fullerene_spool")));
        Golgatha_IMarket.addIndustry("farming", new ArrayList<String>(Arrays.asList("soil_nanites")));
        Golgatha_IMarket.addIndustry("lightindustry", new ArrayList<String>(Arrays.asList("biofactory_embryo")));
        FullName LanaFullName = new FullName("Lana", "Fiel", FullName.Gender.FEMALE);
        String SpritePath = "graphics/portraits/special/FSD_Lana.png";
        Golgatha_IMarket.getCommDirectory().addPerson(FSD_Lana.createPerson("Farsight_Drive",SpritePath,LanaFullName));


        float baradAngle = 100;

        SectorEntityToken FSD_baradL4 = system.addTerrain(Terrain.ASTEROID_FIELD,
                new AsteroidFieldTerrainPlugin.AsteroidFieldParams(
                        500f,
                        700f,
                        20,
                        30,
                        4f,
                        16f,
                        "FSD_Barad L4 Asteroids"));

        SectorEntityToken FSD_baradL5 = system.addTerrain(Terrain.ASTEROID_FIELD,
                new AsteroidFieldTerrainPlugin.AsteroidFieldParams(
                        500f,
                        700f,
                        20,
                        30,
                        4f,
                        16f,
                        "FSD_Barad L5 Asteroids"));
        FSD_baradL4.setCircularOrbit(Golgatha_star, baradAngle -60f, 7800, 400);
        FSD_baradL5.setCircularOrbit(Golgatha_star, baradAngle +60f, 7800, 400);

        PlanetAPI Golgatha_II = system.addPlanet("Golgatha_II",
                Golgatha_star,
                "Mark",
                "terran-eccentric",
                175, 200,
                4500, 255);
        Golgatha_II.setFaction("Farsight_Drive");
        Misc.initConditionMarket(Golgatha_II);
        Golgatha_II.setCustomDescriptionId("Golgatha_II");
        MarketAPI Golgatha_IIMarket;
        Golgatha_IIMarket = addMarketplace(
                "Farsight_Drive",
                Golgatha_II,
                null,
                Golgatha_II.getName(),
                6,
                new ArrayList<>(
                        Arrays.asList(
                                Conditions.POPULATION_6,
                                Conditions.HABITABLE,
                                Conditions.ORE_ABUNDANT,
                                Conditions.RARE_ORE_ABUNDANT,
                                Conditions.COLD,
                                Conditions.FARMLAND_ADEQUATE

                        )),
                new ArrayList<>(
                        Arrays.asList(
                                Submarkets.SUBMARKET_OPEN,
                                Submarkets.SUBMARKET_STORAGE,
                                Submarkets.GENERIC_MILITARY,
                                "FSD_PIMarket"
                        )),
                new ArrayList<>(
                        Arrays.asList(
                                Industries.POPULATION,
                                Industries.SPACEPORT,
                                Industries.MINING,
                                Industries.WAYSTATION,
                                Industries.MILITARYBASE,
                                Industries.HEAVYBATTERIES,
                                Industries.STARFORTRESS_HIGH,
                                "PastItemMilitaryBase"


                        )

                ),
                0.3f,
                false,
                false

        );
        Golgatha_IIMarket.addIndustry("orbitalworks", new ArrayList<String>(Arrays.asList("pristine_nanoforge")));
        Golgatha_IIMarket.addIndustry("heavybatteries", new ArrayList<String>(Arrays.asList("drone_replicator")));
        PlanetAPI Golgatha_III = system.addPlanet("Golgatha_III", Golgatha_star, "Judah", "barren_venuslike", 300, 220, 12000, 500);
        system.addRingBand(Golgatha_III, "misc", "rings_ice0", 256f, 2, Color.white, 256f, 1050, 45, Terrain.RING, null);
        Golgatha_III.setFaction("Farsight_Drive");
        Misc.initConditionMarket(Golgatha_III);
        Golgatha_III.setCustomDescriptionId("Golgatha_III");
        MarketAPI Golgatha_IIIMarket;
        Golgatha_IIIMarket = addMarketplace(
                "pirates",
                Golgatha_III,
                null,
                Golgatha_III.getName(),
                5,
                new ArrayList<>(
                        Arrays.asList(
                                Conditions.POPULATION_5,
                                Conditions.THIN_ATMOSPHERE,
                                Conditions.POOR_LIGHT,
                                Conditions.COLD,
                                Conditions.ORE_SPARSE,
                                Conditions.VOLATILES_TRACE


                        )),
                new ArrayList<>(
                        Arrays.asList(
                                Submarkets.SUBMARKET_OPEN,
                                Submarkets.SUBMARKET_BLACK,
                                Submarkets.SUBMARKET_STORAGE
                        )),
                new ArrayList<>(
                        Arrays.asList(
                                Industries.POPULATION,
                                Industries.SPACEPORT,
                                Industries.MINING,
                                Industries.HEAVYBATTERIES,
                                Industries.MILITARYBASE

                        )
                ),
                0.4f,
                false,
                false
        );
//        PlanetAPI Golgatha_IV = system.addPlanet("Golgatha_IV", Golgatha_star, "Luga", "desert", 250, 250, 6500, 350);
//        MarketAPI Golgatha_IVMarket;
//        Golgatha_IVMarket = addMarketplace(
//                "Farsight_Drive",
//                Golgatha_IV,
//                4,
//                new ArrayList<>(
//                                Conditions.POPULATION_4,
//                                Conditions.RARE_ORE_ULTRARICH,
//                                Conditions.ORE_ULTRARICH,
        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint("Golgatha_jump", "Reflecting-Light Distortion Wormhole"); // creates the system jump point
        jumpPoint.setCircularOrbit(system.getEntityById("Golgatha"), 245+60, 4500, 200);
        jumpPoint.setRelatedPlanet(Golgatha_II);
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint);

        SectorEntityToken gate = system.addCustomEntity("Golgatha_gate",
                "Golgatha Gate",
                "inactive_gate",
                null);
        gate.setCircularOrbit(system.getEntityById("Golgatha"), 0, 6000, 350);


        system.autogenerateHyperspaceJumpPoints(true, true);
        MagicCampaign.hyperspaceCleanup(system);
    }


}

