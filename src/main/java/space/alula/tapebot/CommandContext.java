package space.alula.tapebot;

import com.mewna.catnip.Catnip;
import com.mewna.catnip.entity.channel.TextChannel;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.guild.Member;
import com.mewna.catnip.entity.message.Embed;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.entity.user.User;
import io.vertx.core.Vertx;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class CommandContext {
    public static final String SUCCESS_ICON = "<:check:314349398811475968>";
    public static final String ERROR_ICON = "<:xmark:314349398824058880>";

    private final List<String> args;
    private final Message message;
    private volatile Message lastMessage;

    public CommandContext(Message message, List<String> parts) {
        this.message = message;
        this.args = parts.stream().skip(1).collect(Collectors.toList());
    }

    public int argCount() {
        return args.size();
    }

    public String arg(int index) {
        return args.get(index);
    }

    public String concatArgs() {
        return String.join(" ", args);
    }

    public String argsFrom(int index) {
        return args.stream().skip(index).collect(Collectors.joining(" "));
    }

    public Catnip catnip() {
        return message.catnip();
    }

    public Vertx vertx() {
        return message.catnip().vertx();
    }

    public int shard() {
        return (int) ((message.guildIdAsLong() >> 22) % message.catnip().shardManager().shardCount());
    }

    public User sender() {
        return message.author();
    }

    public Member member() {
        return message.member();
    }

    public Member selfMember() {
        return message.guild().selfMember();
    }

    public TextChannel channel() {
        return message.channel().asTextChannel();
    }

    public Guild guild() {
        return message.guild();
    }

    public Message message() {
        return message;
    }

    public CompletionStage<Message> success(String content) {
        return send(SUCCESS_ICON + "  |  " + content);
    }

    public CompletionStage<Message> error(String content) {
        return send(ERROR_ICON + "  |  " + content);
    }

    public CompletionStage<Message> send(String message) {
        return this.message.channel().sendMessage(message).thenApply(msg -> {
            this.lastMessage = msg;
            return msg;
        });
    }

    public CompletionStage<Message> edit(String message) {
        if (lastMessage == null) {
            return send(message);
        } else {
            return lastMessage.edit(message).thenApply(msg -> {
                this.lastMessage = msg;
                return msg;
            });
        }
    }

    public CompletionStage<Message> send(MessageOptions options) {
        return this.message.channel().sendMessage(options).thenApply(msg -> {
            this.lastMessage = msg;
            return msg;
        });
    }

    public CompletionStage<Message> edit(MessageOptions options) {
        if (lastMessage == null) {
            return send(options);
        } else {
            return lastMessage.edit(options).thenApply(msg -> {
                this.lastMessage = msg;
                return msg;
            });
        }
    }

    public CompletionStage<Message> send(Embed embed) {
        return message.channel().sendMessage(embed).thenApply(msg -> {
            this.lastMessage = msg;
            return msg;
        });
    }


    public CompletionStage<Message> edit(Embed embed) {
        if (lastMessage == null) {
            return send(embed);
        } else {
            return lastMessage.edit(embed).thenApply(msg -> {
                this.lastMessage = msg;
                return msg;
            });
        }
    }
}
