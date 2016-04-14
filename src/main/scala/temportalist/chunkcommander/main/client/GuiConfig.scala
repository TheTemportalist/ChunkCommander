package temportalist.chunkcommander.main.client

import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import temportalist.chunkcommander.main.common.ChunkCommander
import temportalist.origin.foundation.client.gui.GuiConfigBase

/**
  *
  * Created by TheTemportalist on 1/16/2016.
  *
  * @author TheTemportalist
  */
@SideOnly(Side.CLIENT)
class GuiConfig(guiScreen: GuiScreen) extends GuiConfigBase(guiScreen, ChunkCommander) {}
