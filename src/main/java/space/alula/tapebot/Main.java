package space.alula.tapebot;

import com.mewna.catnip.Catnip;
import com.mewna.catnip.CatnipOptions;
import com.mewna.catnip.cache.CacheFlag;
import com.mewna.catnip.shard.DiscordEvent.Raw;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) {
        var options = new CatnipOptions(Env.require("TOKEN"))
                .disabledEvents(Set.of(Raw.PRESENCE_UPDATE, Raw.VOICE_SERVER_UPDATE, Raw.VOICE_STATE_UPDATE,
                        Raw.USER_UPDATE, Raw.MESSAGE_DELETE_BULK, Raw.TYPING_START, Raw.GUILD_MEMBERS_CHUNK,
                        Raw.GUILD_MEMBER_REMOVE, Raw.GUILD_MEMBER_UPDATE, Raw.GUILD_MEMBER_ADD, Raw.MESSAGE_DELETE,
                        Raw.MESSAGE_UPDATE, Raw.MESSAGE_EMBEDS_UPDATE, Raw.MESSAGE_REACTION_ADD, Raw.MESSAGE_REACTION_REMOVE,
                        Raw.MESSAGE_REACTION_REMOVE_ALL))
                .cacheFlags(Set.of(CacheFlag.DROP_EMOJI, CacheFlag.DROP_GAME_STATUSES, CacheFlag.DROP_VOICE_STATES))
                .logUncachedPresenceWhenNotChunking(false)
                .chunkMembers(false);
        var catnip = Catnip.catnip(options);
        var handler = new CommandHandler(catnip);

        handler.registerCommands(Commands.class);

        catnip.connect();
    }
}
