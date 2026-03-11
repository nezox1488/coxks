package fun.rich.events.player;

import fun.rich.utils.client.managers.event.events.callables.EventCancellable;
import net.minecraft.util.Hand;

public class RightClickEvent extends EventCancellable {
    private final Hand hand;
    private final boolean airClick;

    public RightClickEvent(Hand hand, boolean airClick) {
        this.hand = hand;
        this.airClick = airClick;
    }

    public Hand getHand() {
        return hand;
    }

    public boolean isAirClick() {
        return airClick;
    }
}