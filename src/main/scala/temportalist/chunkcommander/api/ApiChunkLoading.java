package temportalist.chunkcommander.api;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by TheTemportalist on 1/14/2016.
 */
public class ApiChunkLoading {

	private static Map<Object, ModContainer> modContainers = new HashMap<Object, ModContainer>();
	private static Map<String, Object> modIDs = new HashMap<String, Object>();
	private static Map<Object, ChunkLoader> chunkLoaders = new HashMap<Object, ChunkLoader>();

	/**
	 * TODO description
	 * To be done prior to postInit
	 */
	public static boolean register(ChunkLoader loader) {
		Object mod = loader.getMod();
		ModContainer modContainer = Loader.instance().getModObjectList().inverse().get(mod);
		if (modContainer == null) return false;
		ApiChunkLoading.modContainers.put(mod, modContainer);
		ApiChunkLoading.modIDs.put(modContainer.getModId(), mod);
		ApiChunkLoading.chunkLoaders.put(mod, loader);
		return true;
	}

	// ~~~~~~~~~~~ Private Use ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static Set<Object> getRegisteredMods() {
		return ApiChunkLoading.modContainers.keySet();
	}

	public static ChunkLoader getChunkLoader(String modID) {
		return ApiChunkLoading.chunkLoaders.get(ApiChunkLoading.modIDs.get(modID));
	}

}
