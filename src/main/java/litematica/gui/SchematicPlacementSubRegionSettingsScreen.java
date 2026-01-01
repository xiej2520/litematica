package litematica.gui;

import java.nio.file.Path;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import malilib.gui.BaseScreen;
import malilib.gui.icon.DefaultIcons;
import malilib.gui.widget.BaseTextFieldWidget;
import malilib.gui.widget.BlockPosEditWidget;
import malilib.gui.widget.CheckBoxWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.button.OnOffButton;
import malilib.util.StringUtils;
import malilib.util.position.BlockMirror;
import malilib.util.position.BlockPos;
import malilib.util.position.BlockRotation;
import malilib.util.position.Coordinate;
import malilib.util.position.Direction;
import litematica.Reference;
import litematica.data.DataManager;
import litematica.schematic.LoadedSchematic;
import litematica.schematic.Schematic;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.placement.SchematicPlacementManager;
import litematica.schematic.placement.SubRegionPlacement;

public class SchematicPlacementSubRegionSettingsScreen extends BaseScreen
{
    protected final SchematicPlacementManager manager;
    protected final SchematicPlacement placement;
    protected final SubRegionPlacement subRegion;

    protected final LabelWidget displayNameLabel;
    protected final LabelWidget originLabel;
    protected final LabelWidget placementNameLabel;
    protected final LabelWidget regionNameLabel;
    protected final LabelWidget schematicNameLabel;
    protected final BaseTextFieldWidget nameTextField;
    protected final GenericButton mirrorButton;
    protected final GenericButton nameResetButton;
    protected final GenericButton openPlacementSettingsButton;
    protected final GenericButton resetSubRegionButton;
    protected final GenericButton rotation1Button;
    protected final GenericButton rotation2Button;
    protected final GenericButton rotationAxis1Button;
    protected final GenericButton rotationAxis2Button;
    protected final GenericButton toggleEntitiesButton;
    protected final GenericButton toggleRegionEnabledButton;
    protected final CheckBoxWidget lockXCoordCheckbox;
    protected final CheckBoxWidget lockYCoordCheckbox;
    protected final CheckBoxWidget lockZCoordCheckbox;
    protected final BlockPosEditWidget originEditWidget;

    public SchematicPlacementSubRegionSettingsScreen(SchematicPlacement placement, SubRegionPlacement subRegion)
    {
        this.placement = placement;
        this.subRegion = subRegion;
        this.manager = DataManager.getSchematicPlacementManager();

        LoadedSchematic loadedSchematic = placement.getLoadedSchematic();
        Schematic schematic = loadedSchematic.schematic;
        Path file = loadedSchematic.file.orElse(null);
        String fileName = file != null ? file.getFileName().toString() : "-";

        this.displayNameLabel   = new LabelWidget("litematica.label.schematic_placement_sub_region_settings.display_name");
        this.originLabel        = new LabelWidget("litematica.label.schematic_placement_sub_region_settings.region_position");
        this.placementNameLabel = new LabelWidget("litematica.label.schematic_placement_sub_region_settings.placement_name", placement.getName());
        this.regionNameLabel    = new LabelWidget("litematica.label.schematic_placement_sub_region_settings.region_name", subRegion.getName());
        this.schematicNameLabel = new LabelWidget("litematica.label.schematic_placement_settings.schematic_name", schematic.getMetadata().getSchematicName(), fileName);
        this.schematicNameLabel.setLineHeight(12);

        this.nameTextField = new BaseTextFieldWidget(300, 16, subRegion.getDisplayName());
        this.nameTextField.setUpdateListenerAlways(false);
        this.nameTextField.setListener(this::setName);

        this.mirrorButton                = GenericButton.create(18, this::getMirrorButtonLabel, this::mirror);
        this.nameResetButton             = GenericButton.create(DefaultIcons.RESET_12, this::resetName);
        this.openPlacementSettingsButton = GenericButton.create(18, "litematica.button.schematic_placement_sub_region_settings.placement_settings", this::openPlacementSettings);
        this.resetSubRegionButton        = GenericButton.create(18, "litematica.button.schematic_placement_sub_region_settings.reset_region", this::resetSubRegion);
        this.rotation1Button             = GenericButton.create(18, this::getRotation1ButtonLabel, this::rotate1);
        this.rotation2Button             = GenericButton.create(18, this::getRotation2ButtonLabel, this::rotate2);
        this.rotationAxis1Button         = GenericButton.create(18, this::getRotationAxis1ButtonLabel, this::cycleRotationAxis1);
        this.rotationAxis2Button         = GenericButton.create(18, this::getRotationAxis2ButtonLabel, this::cycleRotationAxis2);

        this.toggleEntitiesButton      = OnOffButton.onOff(18, "litematica.button.schematic_placement_settings.ignore_entities",
                                                           this.subRegion::ignoreEntities, this::toggleIgnoreEntities);
        this.toggleRegionEnabledButton = OnOffButton.onOff(18, "litematica.button.schematic_placement_sub_region_settings.region_enabled",
                                                           this.subRegion::isEnabled, this::toggleEnabled);

        this.nameResetButton.translateAndAddHoverString("litematica.hover.button.schematic_placement_sub_region_settings.reset_display_name");

        BlockPos pos = this.subRegion.getPosition();
        pos = litematica.util.PositionUtils.getTransformedBlockPos(pos, placement.getMirror(), placement.getRotation()).add(placement.getPosition());
        this.originEditWidget = new BlockPosEditWidget(90, 72, 2, true, pos, this::setOrigin);
        this.originEditWidget.setUpdateImmediateFromTextInput(true);

        this.lockXCoordCheckbox = new CheckBoxWidget(null, null, () -> this.isCoordinateLocked(Coordinate.X), (val) -> this.setCoordinateLocked(val, Coordinate.X));
        this.lockYCoordCheckbox = new CheckBoxWidget(null, null, () -> this.isCoordinateLocked(Coordinate.Y), (val) -> this.setCoordinateLocked(val, Coordinate.Y));
        this.lockZCoordCheckbox = new CheckBoxWidget(null, null, () -> this.isCoordinateLocked(Coordinate.Z), (val) -> this.setCoordinateLocked(val, Coordinate.Z));
        this.lockXCoordCheckbox.translateAndAddHoverString("litematica.hover.checkmark.schematic_placement_settings.lock_coordinate");
        this.lockYCoordCheckbox.translateAndAddHoverString("litematica.hover.checkmark.schematic_placement_settings.lock_coordinate");
        this.lockZCoordCheckbox.translateAndAddHoverString("litematica.hover.checkmark.schematic_placement_settings.lock_coordinate");

        BooleanSupplier enabledSupplier = this::isNotLocked;
        this.rotation1Button.setEnabledStatusSupplier(enabledSupplier);
        this.rotation2Button.setEnabledStatusSupplier(enabledSupplier);
        this.mirrorButton.setEnabledStatusSupplier(enabledSupplier);
        this.originEditWidget.setEnabledStatusSupplier(enabledSupplier);
        this.resetSubRegionButton.setEnabledStatusSupplier(() -> this.subRegion.isRegionPlacementModifiedFromDefault() && this.isNotLocked());

        this.setTitle("litematica.title.screen.schematic_placement_sub_region_settings", Reference.MOD_VERSION);
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.displayNameLabel);
        this.addWidget(this.lockXCoordCheckbox);
        this.addWidget(this.lockYCoordCheckbox);
        this.addWidget(this.lockZCoordCheckbox);
        this.addWidget(this.mirrorButton);
        this.addWidget(this.nameResetButton);
        this.addWidget(this.nameTextField);
        this.addWidget(this.openPlacementSettingsButton);
        this.addWidget(this.originEditWidget);
        this.addWidget(this.originLabel);
        this.addWidget(this.placementNameLabel);
        this.addWidget(this.regionNameLabel);
        this.addWidget(this.resetSubRegionButton);
        this.addWidget(this.rotation1Button);
        this.addWidget(this.rotation2Button);
        this.addWidget(this.rotationAxis1Button);
        this.addWidget(this.rotationAxis2Button);
        this.addWidget(this.schematicNameLabel);
        this.addWidget(this.toggleEntitiesButton);
        this.addWidget(this.toggleRegionEnabledButton);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + 12;
        int y = this.y + 25;
        int toggleButtonY = this.y + 16;

        this.displayNameLabel.setPosition(x + 2, y + 4);
        this.nameTextField.setPosition(this.displayNameLabel.getRight() + 4, y);
        this.nameTextField.setWidth(Math.min(240, this.screenWidth - 300));
        this.nameResetButton.setX(this.nameTextField.getRight() + 2);
        this.nameResetButton.centerVerticallyInside(this.nameTextField);

        this.regionNameLabel.setPosition(x + 2, this.nameTextField.getBottom() + 2);
        this.placementNameLabel.setPosition(x + 2, this.regionNameLabel.getBottom() + 2);
        this.schematicNameLabel.setPosition(x + 2, this.placementNameLabel.getBottom() + 2);

        x = this.getRight() - 114;
        this.toggleRegionEnabledButton.setPosition(x, toggleButtonY);
        this.toggleEntitiesButton.setPosition(x, this.toggleRegionEnabledButton.getBottom() + 1);

        this.originLabel.setPosition(x + 2, this.toggleEntitiesButton.getBottom() + 4);
        this.originEditWidget.setPosition(x + 2, this.originLabel.getBottom() + 1);

        this.rotationAxis1Button.setPosition(x, this.originEditWidget.getBottom() + 1);
        this.rotationAxis2Button.setPosition(x, this.rotationAxis1Button.getBottom() + 1);

        this.rotation1Button.setPosition(this.rotationAxis1Button.getRight() + 2, this.rotationAxis1Button.getY());
        this.rotation2Button.setPosition(this.rotation1Button.getX(), this.rotation1Button.getBottom() + 1);
        this.mirrorButton.setPosition(this.rotation1Button.getX(), this.rotation2Button.getBottom() + 1);

        this.resetSubRegionButton.setPosition(x, this.mirrorButton.getBottom() + 1);

        x = this.originEditWidget.getRight() + 2;
        y = this.originEditWidget.getY();
        this.lockXCoordCheckbox.setPosition(x, y + 2);
        this.lockYCoordCheckbox.setPosition(x, y + 20);
        this.lockZCoordCheckbox.setPosition(x, y + 38);

        y = this.getBottom() - 20;
        this.openPlacementSettingsButton.setPosition(this.x + 10, y);
    }

    protected boolean isNotLocked()
    {
        return this.placement.isLocked() == false;
    }

    protected boolean isCoordinateLocked(Coordinate coordinate)
    {
        return this.subRegion.isCoordinateLocked(coordinate);
    }

    protected void setCoordinateLocked(boolean locked, Coordinate coordinate)
    {
        this.subRegion.setCoordinateLocked(coordinate, locked);
    }

    protected void openPlacementSettings()
    {
        BaseScreen.openScreenWithParent(new SchematicPlacementSettingsScreen(this.placement));
    }

    protected void setName(String name)
    {
        this.subRegion.setDisplayName(name);
    }

    protected void resetName()
    {
        this.subRegion.setDisplayName(this.subRegion.getName());
        this.nameTextField.setText(this.subRegion.getDisplayName());
    }

    protected void setOrigin(BlockPos origin)
    {
        this.manager.moveSubRegionTo(this.placement, this.subRegion.getName(), origin);
    }

    protected boolean mirror(int mouseButton, GenericButton button)
    {
        String name = this.subRegion.getName();

        if (mouseButton == 2)
        {
            this.manager.setSubRegionMirror(this.placement, name, BlockMirror.NONE);
        }
        else
        {
            boolean reverse = mouseButton == 1;
            BlockMirror mirror = this.subRegion.getMirror().cycle(reverse);
            this.manager.setSubRegionMirror(this.placement, name, mirror);
        }

        return true;
    }

    protected boolean rotate1(int mouseButton, GenericButton button)
    {
        String name = this.subRegion.getName();

        if (mouseButton == 2)
        {
            this.manager.setSubRegionRotation1(this.placement, name, BlockRotation.NONE);
        }
        else
        {
            boolean reverse = mouseButton == 1;
            this.manager.setSubRegionRotation1(this.placement, name, this.subRegion.getRotation1().cycle(reverse));
        }

        return true;
    }

    protected boolean rotate2(int mouseButton, GenericButton button)
    {
        String name = this.subRegion.getName();

        if (mouseButton == 2)
        {
            this.manager.setSubRegionRotation2(this.placement, name, BlockRotation.NONE);
        }
        else
        {
            boolean reverse = mouseButton == 1;
            this.manager.setSubRegionRotation2(this.placement, name, this.subRegion.getRotation2().cycle(reverse));
        }

        return true;
    }

    protected boolean cycleRotationAxis1(int mouseButton, GenericButton button)
    {
        String name = this.subRegion.getName();

        if (mouseButton == 2)
        {
            this.manager.setSubRegionRotation1Axis(this.placement, name, Direction.Axis.Y);
        }
        else
        {
            boolean reverse = mouseButton == 1;
            this.manager.setSubRegionRotation1Axis(this.placement, name, this.subRegion.getRotation1Axis().cycle(reverse));
        }

        return true;
    }

    protected boolean cycleRotationAxis2(int mouseButton, GenericButton button)
    {
        String name = this.subRegion.getName();

        if (mouseButton == 2)
        {
            this.manager.setSubRegionRotation2Axis(this.placement, name, Direction.Axis.Y);
        }
        else
        {
            boolean reverse = mouseButton == 1;
            this.manager.setSubRegionRotation2Axis(this.placement, name, this.subRegion.getRotation2Axis().cycle(reverse));
        }

        return true;
    }

    protected void resetSubRegion()
    {
        this.manager.resetSubRegionToSchematicValues(this.placement, this.subRegion.getName());
        BlockPos pos = this.subRegion.getPosition();
        pos = litematica.util.PositionUtils.getTransformedBlockPos(pos, this.placement.getMirror(), this.placement.getRotation()).add(this.placement.getPosition());
        this.originEditWidget.setPosNoUpdate(pos);
        this.updateWidgetStates();
    }

    protected void toggleEnabled()
    {
        this.manager.toggleSubRegionEnabled(this.placement, this.subRegion.getName());
    }

    protected void toggleIgnoreEntities()
    {
        this.manager.toggleSubRegionIgnoreEntities(this.placement, this.subRegion.getName());
    }

    protected String getMirrorButtonLabel()
    {
        String val = this.subRegion.getMirror().getDisplayName();
        String key = "litematica.button.schematic_placement_settings.mirror_value";
        return StringUtils.translate(key, val);
    }

    protected String getRotation1ButtonLabel()
    {
        String val = this.subRegion.getRotation1().getDisplayName();
        String key = "litematica.button.schematic_placement_settings.rotation_1_value";
        return StringUtils.translate(key, val);
    }

    protected String getRotation2ButtonLabel()
    {
        String val = this.subRegion.getRotation2().getDisplayName();
        String key = "litematica.button.schematic_placement_settings.rotation_2_value";
        return StringUtils.translate(key, val);
    }

    protected String getRotationAxis1ButtonLabel()
    {
        String val = this.subRegion.getRotation1Axis().getName().toUpperCase(Locale.ROOT);
        String key = "litematica.button.schematic_placement_settings.axis_value";
        return StringUtils.translate(key, val);
    }

    protected String getRotationAxis2ButtonLabel()
    {
        String val = this.subRegion.getRotation2Axis().getName().toUpperCase(Locale.ROOT);
        String key = "litematica.button.schematic_placement_settings.axis_value";
        return StringUtils.translate(key, val);
    }
}
