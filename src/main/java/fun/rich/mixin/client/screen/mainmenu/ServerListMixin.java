package fun.rich.mixin.client.screen.mainmenu;

import com.llamalad7.mixinextras.sugar.Local;
import fun.rich.features.impl.misc.SelfDestruct;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(ServerList.class)
public class ServerListMixin {
    @Unique
    private final List<ServerInfo> sponsorServers = List.of(
            new ServerInfo("FunTime", "mc.funtime.su", ServerInfo.ServerType.LAN),
            new ServerInfo("HolyWorld", "mc.holyworld.ru", ServerInfo.ServerType.LAN),
            new ServerInfo("ReallyWorld", "mc.reallyworld.ru", ServerInfo.ServerType.LAN),
            new ServerInfo("SpookyTime", "mc.spookytime.net", ServerInfo.ServerType.LAN),
            new ServerInfo("AresMine", "mc.aresmine.ru", ServerInfo.ServerType.LAN)
    );

    @Shadow
    @Final
    private List<ServerInfo> servers;

    @Inject(method = "loadFile", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/ServerList;hiddenServers:Ljava/util/List;", ordinal = 0))
    private void loadFileHook(CallbackInfo ci) {
        if (SelfDestruct.unhooked) return;

        removeDuplicateSponsors();

        for (ServerInfo sponsor : sponsorServers) {
            boolean exists = servers.stream().anyMatch(s -> s.address.equalsIgnoreCase(sponsor.address));
            if (!exists) {
                servers.add(sponsor);
            }
        }
    }

    @Redirect(method = "saveFile", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtList;add(Ljava/lang/Object;)Z", ordinal = 0))
    private boolean saveFileHook(NbtList instance, Object o, @Local(ordinal = 0) ServerInfo info) {
        if (sponsorServers.contains(info)) return true;
        if (SelfDestruct.unhooked) return false;
        return instance.add((NbtElement) o);
    }

    @Unique
    private void removeDuplicateSponsors() {
        Iterator<ServerInfo> iterator = servers.iterator();
        List<String> seenAddresses = new ArrayList<>();

        while (iterator.hasNext()) {
            ServerInfo server = iterator.next();
            if (sponsorServers.stream().anyMatch(s -> s.address.equalsIgnoreCase(server.address))) {
                if (seenAddresses.contains(server.address.toLowerCase())) {
                    iterator.remove();
                } else {
                    seenAddresses.add(server.address.toLowerCase());
                }
            }
        }
    }
}
