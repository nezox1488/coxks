package baritone.behavior;

import baritone.Baritone;
import baritone.api.Settings;
import baritone.api.behavior.ILookBehavior;
import baritone.api.behavior.look.IAimProcessor;
import baritone.api.behavior.look.ITickableAimProcessor;
import baritone.api.event.events.*;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import baritone.behavior.look.ForkableRandom;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.Random;

public final class LookBehavior extends Behavior implements ILookBehavior {

    private Target target;
    private Rotation serverRotation;
    private Rotation prevRotation;
    private final AimProcessor processor;
    private final Deque<Float> smoothYawBuffer;
    private final Deque<Float> smoothPitchBuffer;

    private final Random legitRandom = new Random();
    private float smoothingFactor = 0.22f;

    public LookBehavior(Baritone baritone) {
        super(baritone);
        this.processor = new AimProcessor(baritone.getPlayerContext());
        this.smoothYawBuffer = new ArrayDeque<>();
        this.smoothPitchBuffer = new ArrayDeque<>();
    }

    @Override
    public void updateTarget(Rotation rotation, boolean blockInteract) {
        this.target = new Target(rotation, Target.Mode.resolve(ctx, blockInteract));
    }

    @Override
    public IAimProcessor getAimProcessor() {
        return this.processor;
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.IN) {
            this.processor.tick();
        }
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.target == null) return;

        switch (event.getState()) {
            case PRE: {
                if (this.target.mode == Target.Mode.NONE) return;

                Rotation targetRot = this.target.rotation;
                float currentYaw = ctx.player().getYRot();
                float currentPitch = ctx.player().getXRot();

                float diffYaw = Rotation.normalizeYaw(targetRot.getYaw() - currentYaw);
                float diffPitch = targetRot.getPitch() - currentPitch;

                float maxTurnPerTick = 16.0f + (legitRandom.nextFloat() * 4.0f);

                float stepYaw = diffYaw * (smoothingFactor + legitRandom.nextFloat() * 0.08f);
                float stepPitch = diffPitch * (smoothingFactor + legitRandom.nextFloat() * 0.08f);

                stepYaw = Math.max(-maxTurnPerTick, Math.min(maxTurnPerTick, stepYaw));
                stepPitch = Math.max(-maxTurnPerTick, Math.min(maxTurnPerTick, stepPitch));

                this.prevRotation = new Rotation(currentYaw, currentPitch);

                ctx.player().setYRot(currentYaw + stepYaw);
                ctx.player().setXRot(currentPitch + stepPitch);
                break;
            }
            case POST: {
                if (this.prevRotation != null) {
                    this.smoothYawBuffer.addLast(this.target.rotation.getYaw());
                    while (this.smoothYawBuffer.size() > Baritone.settings().smoothLookTicks.value) {
                        this.smoothYawBuffer.removeFirst();
                    }
                    this.smoothPitchBuffer.addLast(this.target.rotation.getPitch());
                    while (this.smoothPitchBuffer.size() > Baritone.settings().smoothLookTicks.value) {
                        this.smoothPitchBuffer.removeFirst();
                    }

                    if (this.target.mode == Target.Mode.SERVER) {
                        ctx.player().setYRot(this.prevRotation.getYaw());
                        ctx.player().setXRot(this.prevRotation.getPitch());
                    }
                    this.prevRotation = null;
                }
                this.target = null;
                break;
            }
        }
    }

    @Override
    public void onSendPacket(PacketEvent event) {
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket)) return;
        final ServerboundMovePlayerPacket packet = (ServerboundMovePlayerPacket) event.getPacket();
        if (packet instanceof ServerboundMovePlayerPacket.Rot || packet instanceof ServerboundMovePlayerPacket.PosRot) {
            this.serverRotation = new Rotation(packet.getYRot(0.0f), packet.getXRot(0.0f));
        }
    }

    @Override
    public void onWorldEvent(WorldEvent event) {
        this.serverRotation = null;
        this.target = null;
    }

    @Override
    public void onPlayerRotationMove(RotationMoveEvent event) {
        if (this.target != null) {
            final Rotation actual = this.processor.peekRotation(this.target.rotation);
            event.setYaw(actual.getYaw());
            event.setPitch(actual.getPitch());
        }
    }

    private static final class AimProcessor extends AbstractAimProcessor {
        public AimProcessor(final IPlayerContext ctx) { super(ctx); }
        @Override
        protected Rotation getPrevRotation() { return ctx.playerRotations(); }
    }

    private static abstract class AbstractAimProcessor implements ITickableAimProcessor {
        protected final IPlayerContext ctx;
        private final ForkableRandom rand;
        private double randomYawOffset;
        private double randomPitchOffset;

        public AbstractAimProcessor(IPlayerContext ctx) {
            this.ctx = ctx;
            this.rand = new ForkableRandom();
        }

        private AbstractAimProcessor(final AbstractAimProcessor source) {
            this.ctx = source.ctx;
            this.rand = source.rand.fork();
            this.randomYawOffset = source.randomYawOffset;
            this.randomPitchOffset = source.randomPitchOffset;
        }

        @Override
        public final void tick() {
            double jitter = Baritone.settings().randomLooking.value;
            this.randomYawOffset = (this.rand.nextGaussian() * 0.4) * jitter;
            this.randomPitchOffset = (this.rand.nextGaussian() * 0.4) * jitter;

            this.randomYawOffset += (this.rand.nextDouble() - 0.5) * 0.0002;

            double r113 = (this.rand.nextDouble() - 0.5) * Baritone.settings().randomLooking113.value;
            this.randomYawOffset += (Math.abs(r113) < 0.1 ? r113 * 3 : r113);
        }

        @Override
        public final Rotation peekRotation(final Rotation rotation) {
            final Rotation prev = this.getPrevRotation();
            float desiredYaw = rotation.getYaw() + (float)this.randomYawOffset;
            float desiredPitch = rotation.getPitch() + (float)this.randomPitchOffset;

            if (desiredPitch == prev.getPitch()) {
                desiredPitch = nudgeToLevel(desiredPitch);
            }

            return new Rotation(
                    this.calculateMouseMove(prev.getYaw(), desiredYaw),
                    this.calculateMouseMove(prev.getPitch(), desiredPitch)
            ).normalizeAndClamp();
        }

        private float nudgeToLevel(float pitch) {
            float noise = (float)((rand.nextDouble() - 0.5) * 0.006);
            if (pitch < -20) return pitch + 1.0f + noise;
            if (pitch > 10) return pitch - 1.0f + noise;
            return pitch + noise;
        }

        private float calculateMouseMove(float current, float target) {
            float delta = Rotation.normalizeYaw(target - current);

            double sensitivity = ctx.minecraft().options.sensitivity().get() * 0.6 + 0.2;
            double mult = sensitivity * sensitivity * sensitivity * 1.2;

            double jitter = (rand.nextDouble() - 0.5) * 0.15;
            double mouseDelta = (delta / mult) + jitter;

            return current + (float)(mouseDelta * mult);
        }

        @Override public final void advance(int t) { for (int i = 0; i < t; i++) this.tick(); }
        @Override public Rotation nextRotation(Rotation r) { Rotation a = this.peekRotation(r); this.tick(); return a; }
        protected abstract Rotation getPrevRotation();

        @Override public final ITickableAimProcessor fork() {
            return new AbstractAimProcessor(this) {
                private Rotation p = AbstractAimProcessor.this.getPrevRotation();
                @Override public Rotation nextRotation(Rotation r) { return (this.p = super.nextRotation(r)); }
                @Override protected Rotation getPrevRotation() { return this.p; }
            };
        }
    }

    private static class Target {
        public final Rotation rotation;
        public final Mode mode;
        public Target(Rotation r, Mode m) { this.rotation = r; this.mode = m; }
        enum Mode { CLIENT, SERVER, NONE;
            static Mode resolve(IPlayerContext ctx, boolean interact) {
                final Settings s = Baritone.settings();
                if (ctx.player().isFallFlying()) return s.elytraFreeLook.value ? SERVER : CLIENT;
                if (s.freeLook.value) return interact ? (s.blockFreeLook.value ? SERVER : CLIENT) : (s.antiCheatCompatibility.value ? SERVER : NONE);
                return CLIENT;
            }
        }
    }
}
