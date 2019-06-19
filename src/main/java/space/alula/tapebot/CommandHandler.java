package space.alula.tapebot;

import com.mewna.catnip.Catnip;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.shard.DiscordEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    private static final String DEFAULT_PREFIX = "tape ";
    private static final AtomicInteger invokedCommands = new AtomicInteger();

    private final Map<String, MethodHandle> commands;
    private String selfMention;
    private String selfNickMention;

    public CommandHandler(Catnip catnip) {
        commands = new HashMap<>();
        catnip.on(DiscordEvent.MESSAGE_CREATE, this::handleMessage);
    }

    public static int getInvokedCommands() {
        return invokedCommands.get();
    }

    public void registerCommands(Class cls) {
        var methods = Objects.requireNonNull(cls).getDeclaredMethods();
        for (Method method : methods) {
            if (isCommandMethod(method)) {
                try {
                    var annotation = method.getAnnotation(Command.class);
                    var handle = MethodHandles.lookup().unreflect(method);
                    var name = annotation.name().isBlank() ? method.getName() : annotation.name();
                    commands.put(name, handle);
                    logger.debug("Registered command {} -> {}", name, method);
                } catch (Exception e) {
                    logger.error("Failed to register method {} as a command!", method, e);
                }
            }
        }
    }

    private boolean isCommandMethod(Method method) {
        return method.isAnnotationPresent(Command.class)
                && Modifier.isStatic(method.getModifiers())
                && method.getParameterCount() == 1
                && method.getParameterTypes()[0].equals(CommandContext.class);
    }

    private void handleMessage(Message message) {
        if (!message.channel().isGuild() || message.author().bot() || message.content().isBlank()) return;

        if (selfMention == null || selfNickMention == null) {
            var id = message.catnip().clientIdAsLong();
            selfMention = "<@" + id + ">";
            selfNickMention = "<@!" + id + ">";
        }

        var content = message.content();

        if (content.startsWith(DEFAULT_PREFIX)) {
            content = content.substring(DEFAULT_PREFIX.length()).trim();
        } else if (content.startsWith(selfMention)) {
            content = content.substring(selfMention.length()).trim();
        } else if (content.startsWith(selfNickMention)) {
            content = content.substring(selfNickMention.length()).trim();
        } else {
            return;
        }

        var parts = List.of(content.split("\\s"));
        if (parts.isEmpty()) return;

        message.catnip().vertx().executeBlocking(handler -> {
            try {
                var label = parts.get(0).toLowerCase();
                var command = commands.get(label);
                if (command != null) {
                    invokedCommands.incrementAndGet();
                    command.invoke(new CommandContext(message, parts));
                }
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }, result -> {
            if (result.failed()) {
                logger.error("Command invocation failed! [{}] (U:{}, G:{}, C/M:{}/{})",
                        message.content(),
                        message.author().idAsLong(),
                        message.guildIdAsLong(),
                        message.channelIdAsLong(), message.idAsLong(),
                        result.cause());
            }
        });
    }
}
