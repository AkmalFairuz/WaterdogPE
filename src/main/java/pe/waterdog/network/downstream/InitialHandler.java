/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.network.downstream;

import com.nimbusds.jwt.SignedJWT;
import com.nukkitx.protocol.bedrock.packet.*;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import pe.waterdog.network.protocol.ProtocolVersion;
import pe.waterdog.network.rewrite.BlockMap;
import pe.waterdog.network.rewrite.BlockMapModded;
import pe.waterdog.network.rewrite.ItemMap;
import pe.waterdog.network.rewrite.types.BlockPalette;
import pe.waterdog.network.rewrite.types.ItemPalette;
import pe.waterdog.network.rewrite.types.RewriteData;
import pe.waterdog.network.session.SessionInjections;
import pe.waterdog.player.ProxiedPlayer;
import pe.waterdog.utils.exceptions.CancelSignalException;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

public class InitialHandler extends AbstractDownstreamHandler {

    public InitialHandler(ProxiedPlayer player) {
        super(player);
    }

    @Override
    public final boolean handle(ServerToClientHandshakePacket packet) {
        try {
            SignedJWT saltJwt = SignedJWT.parse(packet.getJwt());
            URI x5u = saltJwt.getHeader().getX509CertURL();
            ECPublicKey serverKey = EncryptionUtils.generateKey(x5u.toASCIIString());
            SecretKey key = EncryptionUtils.getSecretKey(
                    this.player.getLoginData().getKeyPair().getPrivate(),
                    serverKey,
                    Base64.getDecoder().decode(saltJwt.getJWTClaimsSet().getStringClaim("salt"))
            );
            this.player.getServer().getDownstream().enableEncryption(key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ClientToServerHandshakePacket clientToServerHandshake = new ClientToServerHandshakePacket();
        this.player.getServer().sendPacket(clientToServerHandshake);
        throw CancelSignalException.CANCEL;
    }

    @Override
    public final boolean handle(ResourcePacksInfoPacket packet) {
        if (!this.player.getProxy().getConfiguration().enabledResourcePacks() || !this.player.acceptResourcePacks()) {
            return false;
        }
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.HAVE_ALL_PACKS);
        this.player.getServer().getDownstream().sendPacketImmediately(response);
        throw CancelSignalException.CANCEL;
    }

    @Override
    public final boolean handle(ResourcePackStackPacket packet) {
        if (!this.player.getProxy().getConfiguration().enabledResourcePacks() || !this.player.acceptResourcePacks()) {
            return false;
        }
        ResourcePackClientResponsePacket response = new ResourcePackClientResponsePacket();
        response.setStatus(ResourcePackClientResponsePacket.Status.COMPLETED);
        this.player.getServer().getDownstream().sendPacketImmediately(response);
        throw CancelSignalException.CANCEL;
    }

    @Override
    public final boolean handle(StartGamePacket packet) {
        RewriteData rewriteData = this.player.getRewriteData();
        rewriteData.setOriginalEntityId(packet.getRuntimeEntityId());
        rewriteData.setEntityId(ThreadLocalRandom.current().nextInt(10000, 15000));
        rewriteData.setGameRules(packet.getGamerules());
        rewriteData.setDimension(packet.getDimensionId());
        rewriteData.setSpawnPosition(packet.getPlayerPosition());

        // Starting with 419 server does not send vanilla blocks to client
        // But thanks to some downstreams we have now item palette rewrite
        if (this.player.getProtocol().getProtocol() <= ProtocolVersion.MINECRAFT_PE_1_16_20.getProtocol()){
            BlockPalette palette = BlockPalette.getPalette(packet.getBlockPalette(), this.player.getProtocol());
            rewriteData.setBlockPalette(palette);
            rewriteData.setBlockPaletteRewrite(palette.createRewrite(palette));
            this.player.getRewriteMaps().setBlockMap(new BlockMap(this.player));
            rewriteData.setShieldBlockingId(ItemPalette.OLD_SHIELD_ID);
        }else {
            rewriteData.setBlockProperties(packet.getBlockProperties());
            this.player.getRewriteMaps().setBlockMap(new BlockMapModded(this.player));

            ItemPalette palette = ItemPalette.getPalette(packet.getItemEntries(), this.player.getProtocol());
            if (this.player.getProxy().getConfiguration().isItemRewrite()){
                rewriteData.setItemPalette(palette);
                rewriteData.setItemPaletteRewrite(palette.createRewrite(palette));

                this.player.getRewriteMaps().setItemMap(new ItemMap(this.player, false));
                this.player.getRewriteMaps().setItemMapReversed(new ItemMap(this.player, true));
            }else {
                // We use shield blocking id from first palette and assume all other servers use same palette
                rewriteData.setShieldBlockingId((int) palette.getShieldBlockingId());
            }
        }

        this.player.setCanRewrite(true);

        packet.setRuntimeEntityId(rewriteData.getEntityId());
        packet.setUniqueEntityId(rewriteData.getEntityId());

        SessionInjections.injectInitialHandlers(this.player.getServer(), player);;
        return true;
    }
}
