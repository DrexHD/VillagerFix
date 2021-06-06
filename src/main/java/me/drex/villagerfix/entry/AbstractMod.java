package me.drex.villagerfix.entry;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.drex.villagerfix.VillagerFix;
import me.drex.villagerfix.commands.Commands;
import me.drex.villagerfix.config.Config;
import me.drex.villagerfix.util.Helper;
import me.drex.villagerfix.villager.types.JsonSerializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;


public abstract class AbstractMod {

    public static final Path DATA_PATH = FabricLoader.getInstance().getConfigDir().resolve("VillagerData");
    public static final Logger LOGGER = LogManager.getFormatterLogger("VillagerFix");

    public void load() {
        LOGGER.info("Initializing VillagerFix!");
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            new Commands().register(dispatcher);
        });
        new VillagerFix();
        initializeVillagerData();
    }

    public void initializeVillagerData() {
        DATA_PATH.toFile().mkdirs();
        for (Map.Entry<VillagerProfession, Int2ObjectMap<TradeOffers.Factory[]>> entry : TradeOffers.PROFESSION_TO_LEVELED_TRADE.entrySet()) {
            saveDataToFile(Helper.toName(entry.getKey()), entry.getValue());
        }
        saveDataToFile("Wandering_Trader", TradeOffers.WANDERING_TRADER_TRADES);
        saveDataToFile("Nitwit", new Int2ObjectArrayMap<>());
    }

    private void saveDataToFile(String fileName, Int2ObjectMap<TradeOffers.Factory[]> data) {
        JSONArray jsonArr = new JSONArray();
        for (int i = 1; i <= data.size(); i++) {
            TradeOffers.Factory[] tradeOffers = data.get(i);
            JSONArray jsonArray = new JSONArray();
            for (TradeOffers.Factory factory : tradeOffers) {
                JSONObject jsonObject;
                if (factory instanceof JsonSerializer) {
                    jsonObject = ((JsonSerializer) factory).toJson();
                } else {
                    try {
                        /*
                        * Note:
                        * We are intentionally passing null values, to fail and inform the user that a non parsable TradeOfferFactory was found.
                        * */
                        TradeOffer tradeOffer = factory.create(null, null);
                        jsonObject = new JSONObject();
                        jsonObject.put("type", "custom");
                        jsonObject.put("firstBuy", JsonSerializer.parseItemStack(tradeOffer.getOriginalFirstBuyItem()));
                        jsonObject.put("secondBuy", JsonSerializer.parseItemStack(tradeOffer.getSecondBuyItem()));
                        jsonObject.put("sell", JsonSerializer.parseItemStack(tradeOffer.getSellItem()));
                        jsonObject.put("max_uses", tradeOffer.getMaxUses());
                        jsonObject.put("experience", tradeOffer.getMerchantExperience());
                        jsonObject.put("multiplier", tradeOffer.getPriceMultiplier());
                    } catch (Exception e) {
                        LOGGER.error("Unable to serialize " + factory.getClass().getSimpleName() + ", ignoring it");
                        continue;
                    }
                }
                jsonArray.put(jsonObject);
            }
            jsonArr.put(jsonArray);
        }
        try {
            Path path = DATA_PATH.resolve(fileName + ".json");
            if (!path.toFile().exists())
            Files.write(path, jsonArr.toString(4).getBytes());
        } catch (Exception e) {
            VillagerFix.LOG.error("Couldn't save " + fileName + ".json", e);
        }
    }


    public void loadConfig() {
        if (!Config.isConfigLoaded) {
            Config.load();
        }
    }
}
