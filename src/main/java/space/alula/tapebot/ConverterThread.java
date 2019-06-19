package space.alula.tapebot;

import com.mewna.catnip.entity.message.MessageOptions;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ConverterThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ConverterThread.class);
    private final CommandContext context;
    private final AudioTrack track;
    private volatile boolean converting;

    public ConverterThread(CommandContext context, AudioTrack track) {
        super("Converter thread [" + context.sender().idAsLong() + "/" + context.guild().idAsLong() + " - " + track.getInfo().title + "]");
        this.context = context;
        this.track = track;
    }

    @Override
    public void run() {
        var player = MusicResolver.playerManager.createPlayer();
        logger.info("Starting conversion...");

        try (var stream = new ByteArrayOutputStream()) {
            player.addListener(new AudioEventAdapter() {
                @Override
                public void onTrackStart(AudioPlayer player, AudioTrack unused) {
                    converting = true;
                    synchronized (track) {
                        track.notifyAll();
                    }
                }

                @Override
                public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
                    converting = false;
                }

                @Override
                public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
                    converting = false;
                    ConverterThread.this.interrupt();
                    context.error("Conversion failed: `" + exception.getMessage() + "`");
                }

                @Override
                public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
                    converting = false;
                    ConverterThread.this.interrupt();
                    context.error("Conversion failed, track got stuck while converting.");
                }
            });

            player.playTrack(track);
            while (!converting) {
                synchronized (track) {
                    track.wait();
                }
            }

            var samples = StandardAudioDataFormats.DISCORD_PCM_S16_LE.maximumChunkSize();
            var converter = new DFPWM(true);
            var frame = new MutableAudioFrame();
            var buffer = ByteBuffer.allocate(samples);
            frame.setBuffer(buffer);

            var eightBitBuffer = new byte[samples / 4];
            var dfpwmBuffer = new byte[eightBitBuffer.length / 8];

            while (converting) {
                if (stream.size() > 8 * 1000000) {
                    throw new IOException("Limit exceeded.");
                }

                if (!player.provide(frame)) {
                    Thread.sleep(2);
                    continue;
                }

                for (int i = 0; i < eightBitBuffer.length; i++) {
                    eightBitBuffer[i] = buffer.get(i * 4 + 1);
                }

                converter.compress(dfpwmBuffer, eightBitBuffer, 0, 0, dfpwmBuffer.length);
                stream.writeBytes(dfpwmBuffer);
                Thread.sleep(2);
            }

            logger.info("Conversion completed, size: {}", stream.size());

            var filename = track.getInfo().title.replaceAll("[?\\s*]", "_") + ".dfpwm";
            context.send(new MessageOptions()
                    .content(CommandContext.SUCCESS_ICON + "  |  " + context.sender().asMention() + ", here you go:")
                    .addFile(filename, stream.toByteArray()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Conversion failed!", e);
            context.error("Conversion failed: `" + e.getMessage() + "`");
        } finally {
            player.destroy();
        }
    }
}
