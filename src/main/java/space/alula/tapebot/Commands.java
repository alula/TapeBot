package space.alula.tapebot;

import com.mewna.catnip.entity.builder.EmbedBuilder;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Commands {
    private Commands() {
        //
    }

    private static final int maxTrackDuration = 30;
    private static final AtomicInteger convertedTracks = new AtomicInteger();
    private static final Runtime rt = Runtime.getRuntime();

    @Command
    public static void convert(CommandContext ctx) {
        ctx.message().channel().triggerTypingIndicator();
        MusicResolver.resolve(ctx.concatArgs())
                .thenAccept(track -> {
                    if (track.getDuration() > TimeUnit.MINUTES.toMillis(maxTrackDuration)) {
                        throw new IllegalArgumentException("Cannot convert tracks longer than " + maxTrackDuration + " minutes!");
                    }

                    ctx.success("Started conversion of `" + StringUtil.codeEscape(track.getInfo().title)
                            + "`, estimated time to complete: `[" + StringUtil.prettyPeriod(track.getDuration() / 10) + "]`.");
                    new ConverterThread(ctx, track).start();
                })
                .exceptionally(e -> {
                    ctx.error("Error while converting the track: `" + e.getMessage() + "`");
                    return null;
                });
    }

    @Command
    public static void help(CommandContext ctx) {
        ctx.send(new EmbedBuilder()
                .title("TapeBot")
                .description("A tiny Discord bot which converts music from YouTube and other sites to the DFPWM format " +
                        "used in Computronics cassette tapes.\n\n" +

                        "**Available commands:**\n\n" +
                        "`tape help` - obvious, displays this help message\n" +
                        "`tape convert <url|ytsearch:(your youtube query)>` - converts a track from any supported source to the DFPWM format.\n" +
                        "`tape stats` - displays some stats cuz why not\n" +
                        "`tape ping` - pang\n")
                .footer("made by Alula#0001, go yell at her if this meme breaks", null)
                .build());
    }

    @Command
    public static void stats(CommandContext ctx) {
        long total = rt.totalMemory() / 1048576;
        long used = total - (rt.freeMemory() / 1048576);
        ctx.send(new EmbedBuilder()
                .title("Stats")
                .description("Heap usage: " + String.format("`%d MiB` / `%d MiB`", used, total) + "\n" +
                        "Converted tracks: `" + convertedTracks.get() + "`\n" +
                        "Commands executed: `" + CommandHandler.getInvokedCommands() + "`\n" +
                        "Java version: `" + System.getProperty("java.version") + "`")
                .build());
    }

    @Command
    public static void ping(CommandContext ctx) {
        ctx.catnip().shardManager().latency(ctx.shard()).thenAccept(latency -> {
            ctx.send("🏓  |  REST: `...ms` | Gateway: `...ms`").thenAccept(message -> {
                long ping = ctx.message().creationTime().until(message.creationTime(), ChronoUnit.MILLIS);
                message.edit("🏓  |  REST: `" + ping + "ms` | Gateway: `" + latency + "ms`");
            });
        });
    }
}
