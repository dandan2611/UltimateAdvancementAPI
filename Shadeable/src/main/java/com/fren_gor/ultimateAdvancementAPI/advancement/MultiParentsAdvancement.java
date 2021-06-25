package com.fren_gor.ultimateAdvancementAPI.advancement;

import com.fren_gor.ultimateAdvancementAPI.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.server.v1_15_R1.AdvancementProgress;
import net.minecraft.server.v1_15_R1.Criterion;
import net.minecraft.server.v1_15_R1.MinecraftKey;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils.ADV_REWARDS;
import static com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils.getAdvancementCriteria;
import static com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils.getAdvancementProgress;
import static com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils.getAdvancementRequirements;

public abstract class MultiParentsAdvancement extends BaseAdvancement {

    // Every parent goes here. Ignoring the parent in BaseAdvancement
    private final Map<BaseAdvancement, FakeAdvancement> parents;
    private final Map<String, Criterion> advCriteria = getAdvancementCriteria(maxCriteria);
    private final String[][] advRequirements = getAdvancementRequirements(advCriteria);
    private final net.minecraft.server.v1_15_R1.AdvancementDisplay mcDisplay;

    public MultiParentsAdvancement(@NotNull AdvancementTab advancementTab, @NotNull String key, @NotNull AdvancementDisplay display, @NotNull BaseAdvancement... parents) {
        this(advancementTab, key, display, 1, parents);
    }

    public MultiParentsAdvancement(@NotNull AdvancementTab advancementTab, @NotNull String key, @NotNull AdvancementDisplay display, @Range(from = 1, to = Integer.MAX_VALUE) int maxCriteria, @NotNull BaseAdvancement... parents) {
        this(advancementTab, key, display, maxCriteria, Sets.newHashSet(Objects.requireNonNull(parents)));
    }

    public MultiParentsAdvancement(@NotNull AdvancementTab advancementTab, @NotNull String key, @NotNull AdvancementDisplay display, @NotNull Set<BaseAdvancement> parents) {
        this(advancementTab, key, display, 1, parents);
    }

    public MultiParentsAdvancement(@NotNull AdvancementTab advancementTab, @NotNull String key, @NotNull AdvancementDisplay display, @Range(from = 1, to = Integer.MAX_VALUE) int maxCriteria, @NotNull Set<BaseAdvancement> parents) {
        super(advancementTab, key, display, validateAndGetFirst(parents), maxCriteria);

        this.parents = Maps.newHashMapWithExpectedSize(parents.size());

        for (BaseAdvancement advancement : parents) {
            if (advancement == null) {
                this.parents.clear();
                throw new IllegalArgumentException("An advancement is null.");
            }
            FakeAdvancement adv = new FakeAdvancement(advancementTab, advancement, display.getX(), display.getY());
            this.parents.put(advancement, adv);
        }
        mcDisplay = display.getMinecraftDisplay(this);
    }

    private static BaseAdvancement validateAndGetFirst(Set<BaseAdvancement> advs) {
        Validate.notNull(advs, "Parent advancements are null.");
        Validate.isTrue(advs.size() > 1, "There must be at least 2 parents.");
        return Objects.requireNonNull(advs.iterator().next());
    }

    @Override
    public void onUpdate(@NotNull UUID uuid, @NotNull Set<net.minecraft.server.v1_15_R1.Advancement> advancementList, @NotNull Map<MinecraftKey, AdvancementProgress> progresses, @NotNull TeamProgression teamProgression, @NotNull Set<MinecraftKey> added) {
        if (isVisible(uuid)) {
            BaseAdvancement tmp = null;
            for (Entry<BaseAdvancement, FakeAdvancement> e : parents.entrySet()) {
                if (e.getKey().isVisible(uuid)) {
                    if (tmp == null)
                        tmp = e.getKey();
                    else
                        e.getValue().onUpdate(uuid, advancementList, progresses, teamProgression, added);
                }
            }
            if (tmp == null) {
                tmp = getParent();
            }
            net.minecraft.server.v1_15_R1.Advancement mcAdv = getMinecraftAdvancement(tmp);
            advancementList.add(mcAdv);
            MinecraftKey key = getMinecraftKey();
            added.add(key);
            progresses.put(key, getAdvancementProgress(mcAdv, teamProgression.getCriteria(this)));
        }
    }

    @NotNull
    @Unmodifiable
    public Set<@NotNull BaseAdvancement> getParents() {
        Set<BaseAdvancement> set = Sets.newHashSetWithExpectedSize(parents.size() + 1);
        set.add((BaseAdvancement) parent);
        set.addAll(parents.keySet());
        return Collections.unmodifiableSet(set);
    }

    public boolean isEveryParentGranted(@NotNull UUID uuid) {
        Validate.notNull(uuid, "Player cannot be null.");
        if (!parent.isGranted(uuid)) {
            return false;
        }
        for (BaseAdvancement advancement : parents.keySet()) {
            if (!advancement.isGranted(uuid)) {
                return false;
            }
        }
        return true;
    }

    public boolean isAnyParentGranted(@NotNull UUID uuid) {
        Validate.notNull(uuid, "Player cannot be null.");
        if (parent.isGranted(uuid)) {
            return true;
        }
        for (BaseAdvancement advancement : parents.keySet()) {
            if (advancement.isGranted(uuid)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAnyParentStarted(@NotNull UUID uuid) {
        Validate.notNull(uuid, "Player cannot be null.");

        TeamProgression pro = advancementTab.getDatabaseManager().getProgression(uuid);

        if (pro.getCriteria(parent) > 0) {
            return true;
        }
        for (BaseAdvancement advancement : parents.keySet()) {
            if (pro.getCriteria(advancement) > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isEveryGrandparentGranted(@NotNull UUID uuid) {
        Validate.notNull(uuid, "Player cannot be null.");

        if (!parent.isGranted(uuid)) {
            BaseAdvancement parent = getParent();
            if (parent instanceof MultiParentsAdvancement && !((MultiParentsAdvancement) parent).isEveryParentGranted(uuid)) {
                return false;
            } else if (!parent.getParent().isGranted(uuid)) {
                return false;
            }
        }
        for (BaseAdvancement advancement : parents.keySet()) {
            if (!advancement.isGranted(uuid)) {
                if (advancement instanceof MultiParentsAdvancement && !((MultiParentsAdvancement) advancement).isEveryParentGranted(uuid)) {
                    return false;
                } else if (!advancement.getParent().isGranted(uuid)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isAnyGrandparentGranted(@NotNull UUID uuid) {
        Validate.notNull(uuid, "Player cannot be null.");
        BaseAdvancement parent = getParent();

        if (parent.isGranted(uuid)) {
            return true;
        } else if (parent instanceof MultiParentsAdvancement && ((MultiParentsAdvancement) parent).isAnyParentGranted(uuid)) {
            return true;
        } else if (parent.getParent().isGranted(uuid)) {
            return true;
        }
        for (BaseAdvancement advancement : parents.keySet()) {
            if (advancement.isGranted(uuid)) {
                return true;
            } else if (advancement instanceof MultiParentsAdvancement && ((MultiParentsAdvancement) advancement).isAnyParentGranted(uuid)) {
                return true;
            } else if (advancement.getParent().isGranted(uuid)) {
                return true;
            }
        }
        return true;
    }

    public boolean isAnyGrandparentStarted(@NotNull UUID uuid) {
        Validate.notNull(uuid, "Player cannot be null.");

        TeamProgression pro = advancementTab.getDatabaseManager().getProgression(uuid);
        BaseAdvancement parent = getParent();

        if (pro.getCriteria(parent) > 0 || isParentStarted(pro, parent, uuid)) {
            return true;
        }
        for (BaseAdvancement advancement : parents.keySet()) {
            if (pro.getCriteria(advancement) > 0 || isParentStarted(pro, advancement, uuid)) {
                return true;
            }
        }
        return false;
    }

    private boolean isParentStarted(@NotNull TeamProgression pro, @NotNull BaseAdvancement adv, @NotNull UUID uuid) {
        // Avoid merging if to improve readability
        if (adv instanceof MultiParentsAdvancement && ((MultiParentsAdvancement) adv).isAnyParentStarted(uuid)) {
            return true;
        } else
            return pro.getCriteria(adv.getParent()) > 0;
    }

    @Override
    public void onDispose() {
        super.onDispose();
        for (FakeAdvancement f : parents.values()) {
            f.onDispose();
        }
    }

    @Override
    @NotNull
    public BaseAdvancement getParent() {
        return (BaseAdvancement) parent;
    }

    @NotNull
    public net.minecraft.server.v1_15_R1.Advancement getMinecraftAdvancement(@NotNull BaseAdvancement advancement) {
        return new net.minecraft.server.v1_15_R1.Advancement(getMinecraftKey(), advancement.getMinecraftAdvancement(), mcDisplay, ADV_REWARDS, advCriteria, advRequirements);
    }
}