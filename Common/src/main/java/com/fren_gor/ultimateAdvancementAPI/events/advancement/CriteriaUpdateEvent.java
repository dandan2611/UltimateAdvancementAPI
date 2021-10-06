package com.fren_gor.ultimateAdvancementAPI.events.advancement;

import com.fren_gor.ultimateAdvancementAPI.database.DatabaseManager;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * Called when the criteria progression of an advancement changes.
 * <p>This event differs from {@link AdvancementCriteriaUpdateEvent} because it is called by {@link DatabaseManager#updateCriteriaWithCompletable(AdvancementKey, TeamProgression, int)}.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CriteriaUpdateEvent extends Event {

    @Getter
    private final TeamProgression team;
    @Range(from = 0, to = Integer.MAX_VALUE)
    private final int oldCriteria, newCriteria;
    @NotNull
    private final AdvancementKey advancementKey;

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }
}