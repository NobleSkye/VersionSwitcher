package lionbyte.dev.versionswitcher;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

@Plugin(
    id = "versionswitcher",
    name = "VersionSwitcher",
    version = "1.0.0",
    description = "Change your proxy's protocol version text",
    authors = { "LionByte" }
)
public class Main {

    private final Logger logger;
    private final Path dataDir;
    private final ProxyServer server;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private String protocol = "Velocity Server";
    private int protocolVersion = -1;

    @Inject
    public Main(
        final Logger logger,
        final ProxyServer server,
        @DataDirectory final Path dataDir
    ) {
        this.logger = logger;
        this.server = server;
        this.dataDir = dataDir;
    }

    @Subscribe
    public void onProxyInitialize(final ProxyInitializeEvent event) {
        try {
            loadConfiguration();
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
        }
    }

    private void loadConfiguration() throws IOException {
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }

        Path configFile = dataDir.resolve("config.toml");
        Properties properties = new Properties();

        if (Files.notExists(configFile)) {
            properties.setProperty("protocol", "Custom velocitypowered Server");
            properties.setProperty("version", "-1");
            Files.write(configFile, propertiesToString(properties).getBytes());
        } else {
            try (InputStream is = Files.newInputStream(configFile)) {
                properties.load(is);
            }
            protocol = properties.getProperty(
                "protocol",
                "velocitypowered Server"
            );
            protocolVersion = Integer.parseInt(properties.getProperty("version", "-1"));
        }
    }

    private String propertiesToString(final Properties properties) {
        StringBuilder sb = new StringBuilder();
        properties.forEach((key, value) -> {
            sb.append(key).append("=").append(value).append("\n");
        });
        return sb.toString();
    }

    @Subscribe
    public void onProxyPing(final ProxyPingEvent event) {
        ServerPing ping = event.getPing();
        if (ping == null) {
            return;
        }

        Component component = miniMessage.deserialize(protocol);
        String legacyText = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
            .serialize(component);

        ServerPing.Version version = new ServerPing.Version(
            protocolVersion,
            legacyText
        );

        ServerPing newPing = ping.asBuilder().version(version).build();

        event.setPing(newPing);
    }
}
