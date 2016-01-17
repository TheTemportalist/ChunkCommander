package temportalist.chunkcommander.client

import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard
import temportalist.chunkcommander.common.ChunkCommander
import temportalist.origin.api.common.resource.IModDetails
import temportalist.origin.foundation.client.{EnumKeyCategory, IKeyBinder}

/**
  * Created by TheTemportalist on 1/16/2016.
  */
object ModKeys extends IKeyBinder {

	private var toggleLoadedBoundaries: KeyBinding = null
	private var showChunkBoundaries: KeyBinding = null

	override def register(): Unit = {
		this.toggleLoadedBoundaries = this.makeKeyBinding(
			"toggleLoadedBoundaries", Keyboard.KEY_BACKSLASH, EnumKeyCategory.GAMEPLAY)
		this.showChunkBoundaries = this.makeKeyBinding(
			"showChunkBoundaries", Keyboard.KEY_RBRACKET, EnumKeyCategory.GAMEPLAY)
	}

	override def getMod: IModDetails = ChunkCommander

	override def onKeyPressed(keyBinding: KeyBinding): Unit = {
		if (keyBinding == this.toggleLoadedBoundaries) WorldRender.toggleLoadedBoundaries()
		else if (keyBinding == this.showChunkBoundaries)
			WorldRender.toggleChunkBoundariesAndCorners()
	}

}
