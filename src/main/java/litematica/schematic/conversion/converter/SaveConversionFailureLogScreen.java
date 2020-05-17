package litematica.schematic.conversion.converter;

import litematica.util.LitematicaDirectories;
import malilib.gui.BaseScreen;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.DataListWidget;
import malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import malilib.overlay.message.MessageDispatcher;
import malilib.render.text.StyledTextLine;
import malilib.util.StringUtils;
import malilib.util.datadump.DataDump;

import java.nio.file.Path;
import java.util.List;

public class SaveConversionFailureLogScreen extends BaseScreen {
    protected final List<String> failedStates;
    protected final GenericButton saveButton;
    protected final DataListWidget<String> listWidget;

    public SaveConversionFailureLogScreen(List<String> failedStates)
    {
        this.failedStates = failedStates;

        this.saveButton = GenericButton.create(20, "Save Log", this::saveLog);
        this.listWidget = new DataListWidget<>(() -> this.failedStates, false);

        this.useTitleHierarchy = false;

        this.setTitle("Save conversion failure log");

        this.setScreenWidthAndHeight(640, 400);
        this.centerOnScreen();

        this.listWidget.setDataListEntryWidgetFactory((data, constructData) ->
            new BaseDataListEntryWidget<String>(data, constructData) {
            {
                this.setText(StyledTextLine.parseJoin(data));
            }
        });
        this.listWidget.setSize(this.getScreenWidth() - 24, this.getScreenHeight() - 48);
        this.listWidget.setListEntryWidgetFixedHeight(8);
    }

    protected void saveLog()
    {
        try
        {
            Path dir = LitematicaDirectories.getDataDirectory("conversion_failures");
            Path file = DataDump.dumpDataToFile(dir, "failed_states", ".txt", this.failedStates);
            if (file != null) {
                String msg = "Failure log written to file " + file.getFileName();
                MessageDispatcher.success(msg);
                StringUtils.sendOpenFileChatMessage(msg, file);
            }
        }
        catch (Exception e)
        {
            MessageDispatcher.error().console(e).printToConsole("failed to write failure log");
        }
        this.closeScreenOrShowParent();
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.saveButton);
        this.addWidget(this.listWidget);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + 6;
        int y = this.y + 20;
        this.saveButton.setPosition(x, y);

        this.listWidget.setPosition(x, y + 24);
        this.listWidget.refreshEntries();
        this.listWidget.updateSubWidgetPositions();
    }

}
