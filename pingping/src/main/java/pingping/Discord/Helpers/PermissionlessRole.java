package pingping.Discord.Helpers;

import java.awt.Color;

import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;


public class PermissionlessRole {
    public static Role create(Server server, String name, Color color) {
        return server.createRoleBuilder()
            .setName(name)
            .setColor(color)
            .setPermissions(new PermissionsBuilder().setAllUnset().build())
            .create().join();
    }
}
