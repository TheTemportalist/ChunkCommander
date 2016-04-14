package temportalist.chunkcommander.api;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by TheTemportalist on 1/14/2016.
 *
 * @author TheTemportalist
 */
public abstract class ChunkLoader {

	/**
	 * A map keyed by dimension id's and mapped to tickets which hold all the chunks
	 * being loaded for a dimension by a mod.
	 */
	private Map<Integer, Ticket> dimensionTicketMap = new HashMap<Integer, Ticket>();
	private Map<UUID, Ticket> playerTickets = new HashMap<UUID, Ticket>();

	public abstract Object getMod();

	public void notifyWithTicket(Ticket ticket) {
		this.dimensionTicketMap.put(ticket.world.provider.getDimension(), ticket);
	}

	public final boolean forceLoadChunk(World world, ChunkCoordIntPair chunk, Type type) {
		if (chunk == null || world == null) return false;
		int dimension = world.provider.getDimension();
		Ticket ticket = this.dimensionTicketMap.containsKey(dimension)
				? this.dimensionTicketMap.get(dimension)
				: ForgeChunkManager.requestTicket(this.getMod(), world, type);
		if (ticket == null) return false;
		ForgeChunkManager.forceChunk(ticket, chunk);
		this.dimensionTicketMap.put(dimension, ticket);
		return true;
	}

	public final boolean forceLoadChunk(World world, ChunkCoordIntPair chunk) {
		return this.forceLoadChunk(world, chunk, Type.NORMAL);
	}

	public final boolean unforceChunk(int dimensionID, ChunkCoordIntPair chunk) {
		Ticket ticket = this.dimensionTicketMap.get(dimensionID);
		if (ticket == null) return false;
		ForgeChunkManager.unforceChunk(ticket, chunk);
		if (ticket.getChunkList().isEmpty()) {
			ForgeChunkManager.releaseTicket(ticket);
			this.dimensionTicketMap.remove(dimensionID);
		}
		return true;
	}

	public final boolean forceLoadChunkPlayer(String name, World world, ChunkCoordIntPair chunk) {
		return this.forceLoadChunkPlayer(name, world, chunk, Type.NORMAL);
	}

	public final boolean forceLoadChunkPlayer(String name,
			World world, ChunkCoordIntPair chunk, Type type) {
		MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
		PlayerProfileCache cache = server.getPlayerProfileCache();
		GameProfile profile = cache.getGameProfileForUsername(name);
		UUID uuid = profile == null ? null : profile.getId();
		return this.forceLoadChunkPlayer(name, uuid, world, chunk, type);
	}

	public final boolean forceLoadChunkPlayer(String name, UUID uuid,
			World world, ChunkCoordIntPair chunk) {
		return this.forceLoadChunkPlayer(name, uuid, world, chunk, Type.NORMAL);
	}

	public final boolean forceLoadChunkPlayer(String name, UUID uuid,
			World world, ChunkCoordIntPair chunk, Type type) {
		if (chunk == null || world == null) return false;
		Ticket ticket = this.playerTickets.containsKey(uuid)
				? this.playerTickets.get(uuid)
				: ForgeChunkManager.requestPlayerTicket(this.getMod(), name, world, type);
		if (ticket == null) return false;
		ForgeChunkManager.forceChunk(ticket, chunk);
		this.playerTickets.put(uuid, ticket);
		return true;
	}

	public final boolean unforceChunkPlayer(UUID uuid, ChunkCoordIntPair chunk) {
		Ticket ticket = this.playerTickets.get(uuid);
		if (ticket == null) return false;
		ForgeChunkManager.unforceChunk(ticket, chunk);
		if (ticket.getChunkList().isEmpty()) {
			ForgeChunkManager.releaseTicket(ticket);
			this.playerTickets.remove(uuid);
		}
		return true;
	}

	public boolean shouldPlayerTicketPersist(String playerName, Ticket ticket) {
		return false;
	}

	public boolean shouldContinueForcingChunk(World world, ChunkCoordIntPair chunk) {
		return false;
	}

}
