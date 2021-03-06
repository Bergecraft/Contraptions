package vg.civcraft.mc.contraptions.properties;

import java.util.Set;
import vg.civcraft.mc.contraptions.ContraptionManager;
import vg.civcraft.mc.contraptions.contraptions.Generator;
import vg.civcraft.mc.contraptions.gadgets.ConversionGadget;
import vg.civcraft.mc.contraptions.gadgets.GrowGadget;
import vg.civcraft.mc.contraptions.gadgets.MatchGadget;
import vg.civcraft.mc.contraptions.gadgets.MinMaxGadget;
import vg.civcraft.mc.contraptions.gadgets.TerritoryGadget;
import vg.civcraft.mc.contraptions.utility.Response;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.json.JSONObject;
import vg.civcraft.mc.contraptions.gadgets.StructureGadget;
import vg.civcraft.mc.contraptions.utility.Anchor;
import vg.civcraft.mc.contraptions.utility.BlockLocation;
import vg.civcraft.mc.contraptions.utility.JSONHelpers;
import vg.civcraft.mc.contraptions.utility.SoundType;

public class GeneratorProperties extends ContraptionProperties {

    int period;
    MatchGadget matchGadget;
    TerritoryGadget territoryGadget;
    ConversionGadget conversionGadget;
    GrowGadget generationGadget;
    GrowGadget degredationGadget;
    MinMaxGadget minMaxGadget;

    /**
     * Creates a GeneratorProperties object
     *
     * @param contraptionManager The ContraptionManager
     * @param ID The unique ID for this specification
     * @param matchGadget The MatchGadget associated with this specification
     * @param territoryGadget The TerritoryGadget associated with this
     * specification
     * @param conversionGadget The ConversionGadget associated with this
     * specification
     */
    public GeneratorProperties(ContraptionManager contraptionManager, String ID, String name, StructureGadget structureGadget, int period, MatchGadget matchGadget, TerritoryGadget territoryGadget, ConversionGadget conversionGadget, GrowGadget generationGadget, GrowGadget degredationGadget, MinMaxGadget minMaxGadget) {
        super(contraptionManager, ID, name, structureGadget);
        this.period = period;
        this.matchGadget = matchGadget;
        this.territoryGadget = territoryGadget;
        this.conversionGadget = conversionGadget;
        this.generationGadget = generationGadget;
        this.degredationGadget = degredationGadget;
        this.minMaxGadget = minMaxGadget;
    }

    /**
     * Imports a GeneratorProperties object from a configuration file.
     *
     * The config file should be formatted as follows:
     * <pre>
     * {
     *   name: "ID" //Common name of this generator
     *   generation_rate: 10000 //ItemStacks generated globally per day
     *   degredation_rate: 50 //ItemStacks degraded by a generator per day
     *   capacity: 500 //Amount of excess capacity of ItemStacks the generator will hold
     *   period: 600 //Frequency of updates of the generator in seconds
     *   construction_cost: { ItemSet } //Cost to build Generator
     *   materials: { ItemSet } //Materials generated and consumed by Generator
     * }
     * </pre>
     *
     * @param contraptionManager The ContraptionManager
     * @param ID The Unique ID of this specification
     * @param jsonObject A JSONObject containing the specification
     * @return The specified FactoryProperties file
     */
    public static GeneratorProperties fromConfig(ContraptionManager contraptionManager, String ID, JSONObject jsonObject) {
        String name = JSONHelpers.loadString(jsonObject, "name", ID);
        StructureGadget structureGadget = ContraptionProperties.SG_DEFAULT;
        try {
            structureGadget = StructureGadget.fromJSON(jsonObject.getJSONObject("structure"));
        } catch (Exception e) {

        }
        //In ItemSets per day
        double generationRate = JSONHelpers.loadDouble(jsonObject, "generation_rate", 10000);
        //In ItemSets per day
        double degredationRate = JSONHelpers.loadDouble(jsonObject, "degredation_rate", 50);
        double maxItemStacks = JSONHelpers.loadDouble(jsonObject, "capacity", 500);
        //How frequently to update the generator in seconds
        int period = JSONHelpers.loadInt(jsonObject, "period", 600);
        MatchGadget matchGadget = new MatchGadget(JSONHelpers.loadItemStacks(jsonObject, "construction_cost"));
        Set<ItemStack> repairMaterials = JSONHelpers.loadItemStacks(jsonObject, "materials");
        TerritoryGadget territoryGadget = new TerritoryGadget();
        ConversionGadget conversionGadget = new ConversionGadget(repairMaterials, 1);
        GrowGadget generationGadget = new GrowGadget(generationRate / (24 * 60 * 60 * 20));
        GrowGadget degredationGadget = new GrowGadget(degredationRate / (24 * 60 * 60 * 20));
        MinMaxGadget minMaxGadget = new MinMaxGadget(0, maxItemStacks);
        return new GeneratorProperties(contraptionManager, ID, name, structureGadget, period, matchGadget, territoryGadget, conversionGadget, generationGadget, degredationGadget, minMaxGadget);
    }

    @Override
    public Generator newContraption(Anchor anchor) {
        return new Generator(this, anchor);
    }

    @Override
    public String getType() {
        return "generator";
    }

    /**
     * Creates a Factory Contraptions
     *
     * @param anchor Anchor to attempt creation
     * @return Created Contraption if successful
     */
    @Override
    public Response createContraption(BlockLocation location) {
        //Check if interaction block is correct
        if (structureGadget.validBlock(location.getBlock())) {
            return new Response(false, "Incorrect block for a Factory");
        }
        Anchor anchor = structureGadget.exists(location);
        if (anchor == null) {
            return new Response(false, "Incorrect structure for factory");
        }
        Inventory inventory = ((InventoryHolder) location.getBlock().getState()).getInventory();
        if (matchGadget.matches(inventory) && matchGadget.consume(inventory)) {
            Generator newGenerator = new Generator(this, anchor);
            contraptionManager.registerContraption(newGenerator);
            SoundType.CREATION.play(anchor.getLocation());
            return new Response(true, "Created a " + newGenerator.getName() + " factory!", newGenerator);
        }
        return new Response(false, "Incorrect items for a Factory");
    }

    /**
     * Gets the ConversionGadget
     *
     * @return The ConversionGadget
     */
    public ConversionGadget getConversionGadget() {
        return conversionGadget;
    }

    /**
     * Gets the ProductionGadget
     *
     * @return The ProductionGadget
     */
    public TerritoryGadget getTerritoryGadget() {
        return territoryGadget;
    }

    /**
     * Gets the GrowGadget
     *
     * @return The GrowGadget
     */
    public GrowGadget getGenerationGadget() {
        return generationGadget;
    }

    public GrowGadget getDegradationGadget() {
        return degredationGadget;
    }

    /**
     * Gets the MinMaxGadget
     *
     * @return The MinMaxGadget
     */
    public MinMaxGadget getMinMaxGadget() {
        return minMaxGadget;
    }

    public int getPeriod() {
        return period;
    }

}
