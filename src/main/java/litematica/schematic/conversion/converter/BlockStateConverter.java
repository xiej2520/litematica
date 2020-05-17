package litematica.schematic.conversion.converter;

import com.google.gson.JsonArray;
import litematica.Litematica;
import malilib.gui.BaseScreen;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.Constants;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.ListData;
import malilib.util.game.MinecraftVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import static malilib.util.game.MinecraftVersion.MC_1_12_2;

public class BlockStateConverter extends DataConverterBase
{
    // CompoundData doesn't implement equals, can't use in HashMap
    private HashMap<CompoundData, CompoundData> stateMapping = new HashMap<>();

    private BlockStateConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        super(versionFrom, versionTo);
    }

    private BlockStateConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo, String mappingFileName, String arrayName) {
        super(versionFrom, versionTo);
        Optional<JsonArray> jsonArray = BlockStateMapReader.read(mappingFileName, arrayName);
        if (jsonArray.isPresent())
        {
            this.stateMapping = BlockStateMapReader.read(jsonArray.get(), versionFrom, versionTo);
        }
        else
        {
            Litematica.LOGGER.error("failed to read blockstate map json array");
        }
    }

    public static Optional<BlockStateConverter> get(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        if (versionFrom.dataVersion > MC_1_12_2.dataVersion)
        {
            return Optional.of(new BlockStateConverter(versionFrom, versionTo, "block_state_map.json", "block_states"));
        }

        return Optional.empty();
    }

    /**
     * Converts the provided block state palette.<br>
     * <b>Note:</b> The returned palette is independent of the input palette, all the tags are copied.
     * @param paletteTagIn
     * @return
     */
    public ListData convertedPalette(ListData paletteTagIn)
    {
        final int paletteSize = paletteTagIn.size();
        ListData paletteTagOut = new ListData(Constants.NBT.TAG_COMPOUND);

        ArrayList<String> failedStates = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < paletteSize; ++i)
        {
            CompoundData tag = paletteTagIn.getCompoundAt(i);
            CompoundData convertedTag = this.stateMapping.get(tag);

            if (convertedTag != null)
            {
                //System.out.printf("converted: %s => %s\n", tag, convertedTag);
                paletteTagOut.add(convertedTag.copy());
                ++successCount;
            }
            else
            {
                System.out.printf("FAILED: %s => %s\n", tag, convertedTag);
                failedStates.add(tag.toString());
                paletteTagOut.add(tag.copy());
                ++failCount;
            }
        }

        if (failCount > 0)
        {
            String verFrom = this.versionFrom.displayName;
            String verTo = this.versionTo.displayName;
            String strSu = String.valueOf(successCount);
            String strFa = String.valueOf(failCount);
            MessageDispatcher.warning("litematica.message.warn.schematic_conversion.palette_conversion_failures", verFrom, verTo, strSu, strFa);
            MessageDispatcher.error(String.join("\n", failedStates));
            BaseScreen.openPopupScreen(new SaveConversionFailureLogScreen(failedStates));
        }

        return paletteTagOut;
    }
}
