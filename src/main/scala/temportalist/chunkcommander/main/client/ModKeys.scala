package temportalist.chunkcommander.main.client

import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import org.lwjgl.input.Keyboard
import temportalist.chunkcommander.main.common.ChunkCommander
import temportalist.origin.api.client.EnumKeyCategory
import temportalist.origin.foundation.client.IKeyBinder

/**
  *
  * Created by TheTemportalist on 4/13/2016.
  *
  * @author TheTemportalist
  */
@SideOnly(Side.CLIENT)
object ModKeys extends IKeyBinder {

	private var showChunkBoundaries: KeyBinding = _

	/**
	  * Use this method to create and register your KeyBindings
	  * Make sure to call it in your [[temportalist.origin.foundation.common.IProxy.postInit]]
	  * Use [[registerKeyBinding]] to register a [[KeyBinding]]
	  */
	override def register(): Unit = {

		this.showChunkBoundaries = new KeyBinding("showChunkBoundaries", Keyboard.KEY_BACKSLASH, EnumKeyCategory.GAMEPLAY.getName)
		this.registerKeyBinding(this.showChunkBoundaries)

	}

	/**
	  * Called when a KeyBinding is pressed
	  *
	  * @param keyBinding The [[KeyBinding]]
	  */
	override def onKeyPressed(keyBinding: KeyBinding): Unit = {
		WorldRender.toggleChunkBoundariesAndCorners()
	}

}
