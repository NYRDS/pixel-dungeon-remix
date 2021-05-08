/*
t * Pixel Dungeon
 * Copyright (C) 2012-2014  Oleg Dolya
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.watabou.pixeldungeon;

import com.nyrds.LuaInterface;
import com.nyrds.android.util.FileSystem;
import com.nyrds.android.util.ModdingMode;
import com.nyrds.android.util.TrackedRuntimeException;
import com.nyrds.android.util.Util;
import com.nyrds.lua.LuaEngine;
import com.nyrds.pixeldungeon.ai.MobAi;
import com.nyrds.pixeldungeon.ai.Wandering;
import com.nyrds.pixeldungeon.items.Treasury;
import com.nyrds.pixeldungeon.items.common.Library;
import com.nyrds.pixeldungeon.levels.IceCavesLevel;
import com.nyrds.pixeldungeon.levels.NecroLevel;
import com.nyrds.pixeldungeon.levels.objects.LevelObject;
import com.nyrds.pixeldungeon.ml.EventCollector;
import com.nyrds.pixeldungeon.ml.R;
import com.nyrds.pixeldungeon.mobs.npc.AzuterronNPC;
import com.nyrds.pixeldungeon.mobs.npc.CagedKobold;
import com.nyrds.pixeldungeon.mobs.npc.PlagueDoctorNPC;
import com.nyrds.pixeldungeon.mobs.npc.ScarecrowNPC;
import com.nyrds.pixeldungeon.utils.CharsList;
import com.nyrds.pixeldungeon.utils.DungeonGenerator;
import com.nyrds.pixeldungeon.utils.EntityIdSource;
import com.nyrds.pixeldungeon.utils.Position;
import com.watabou.noosa.Game;
import com.watabou.pixeldungeon.Rankings.gameOver;
import com.watabou.pixeldungeon.actors.Actor;
import com.watabou.pixeldungeon.actors.Char;
import com.watabou.pixeldungeon.actors.buffs.Amok;
import com.watabou.pixeldungeon.actors.hero.Hero;
import com.watabou.pixeldungeon.actors.hero.HeroClass;
import com.watabou.pixeldungeon.actors.mobs.Mob;
import com.watabou.pixeldungeon.actors.mobs.npcs.Blacksmith;
import com.watabou.pixeldungeon.actors.mobs.npcs.Ghost;
import com.watabou.pixeldungeon.actors.mobs.npcs.Imp;
import com.watabou.pixeldungeon.actors.mobs.npcs.WandMaker;
import com.watabou.pixeldungeon.items.Ankh;
import com.watabou.pixeldungeon.items.potions.Potion;
import com.watabou.pixeldungeon.items.rings.Ring;
import com.watabou.pixeldungeon.items.scrolls.Scroll;
import com.watabou.pixeldungeon.items.wands.Wand;
import com.watabou.pixeldungeon.levels.DeadEndLevel;
import com.watabou.pixeldungeon.levels.Level;
import com.watabou.pixeldungeon.levels.Room;
import com.watabou.pixeldungeon.levels.Terrain;
import com.watabou.pixeldungeon.scenes.GameScene;
import com.watabou.pixeldungeon.ui.QuickSlot;
import com.watabou.pixeldungeon.utils.BArray;
import com.watabou.pixeldungeon.utils.GLog;
import com.watabou.pixeldungeon.utils.Utils;
import com.watabou.pixeldungeon.windows.WndResurrect;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;
import com.watabou.utils.SystemTime;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.SneakyThrows;

import static com.watabou.pixeldungeon.RemixedDungeon.MOVE_TIMEOUTS;

public class Dungeon {

    public static int     potionOfStrength;
    public static int     scrollsOfUpgrade;
    public static int     arcaneStyli;
    public static boolean dewVial; // true if the dew vial can be spawned
    public static int     transmutation; // depth number for a well of transmutation

    private static int challenges;

    public static Hero  hero;

    public static Level  level;
    public static String levelId;

    public static  int depth;
    private static final AtomicInteger loading = new AtomicInteger();
    private static long lastSaveTimestamp;

    public static  String  gameId;

    private static boolean realtime;

    private static int     moveTimeoutIndex;

    public static HashSet<Integer> chapters;

    // Hero's field of view
    public static boolean[] visible;

    public static boolean nightMode;

    private static boolean[] passable;

    public static HeroClass heroClass;

    public static void initSizeDependentStuff(int w, int h) {
        int size = w * h;
        Actor.clearActors();

        visible = new boolean[size];
        passable = new boolean[size];

        Arrays.fill(visible, false);

        PathFinder.setMapSize(w, h);
    }

    public static void init() {
        SaveUtils.deleteLevels(heroClass);

        gameId = String.valueOf(SystemTime.now());

        LuaEngine.reset();
        Treasury.reset();

        setChallenges(RemixedDungeon.challenges());

        Scroll.initLabels();
        Potion.initColors();
        Wand.initWoods();
        Ring.initGems();

        Statistics.reset();
        Journal.reset();

        depth = 0;

        potionOfStrength = 0;
        scrollsOfUpgrade = 0;
        arcaneStyli = 0;
        dewVial = true;
        transmutation = Random.IntRange(6, 14);

        chapters = new HashSet<>();

        Ghost.Quest.reset();
        WandMaker.Quest.reset();
        Blacksmith.Quest.reset();
        Imp.Quest.reset();
        ScarecrowNPC.Quest.reset();
        AzuterronNPC.Quest.reset();
        CagedKobold.Quest.reset();
        PlagueDoctorNPC.Quest.reset();

        Room.shuffleTypes();

        hero = new Hero(difficulty);

        Badges.reset();

        heroClass.initHero(hero);

        hero.levelId = DungeonGenerator.getEntryLevel();

        realtime = RemixedDungeon.realtime();
        moveTimeoutIndex = RemixedDungeon.limitTimeoutIndex(RemixedDungeon.moveTimeout());
    }

    @Contract(pure = true)
    public static boolean isChallenged(int mask) {
        return (getChallenges() & mask) != 0;
    }

    private static void updateStatistics() {
        if (depth > Statistics.deepestFloor) {
            Statistics.deepestFloor = depth;

            Statistics.completedWithNoKilling = Statistics.qualifiedForNoKilling;
        }
    }

    @NotNull
    public static Level newLevel(@NotNull Position pos) {
        try {
            loading.incrementAndGet();
            updateStatistics();

            if (!DungeonGenerator.isLevelExist(pos.levelId)) {
                pos.levelId = DungeonGenerator.getEntryLevel();
                pos.cellId = -1;
                pos = DungeonGenerator.descend(pos);
            }

            Level level = DungeonGenerator.createLevel(pos);

            Dungeon.hero.setPos(level.entrance);

            Statistics.qualifiedForNoKilling = !DungeonGenerator.getLevelProperty(level.levelId, "isSafe", false);

            return level;
        } finally {
            loading.decrementAndGet();
        }
    }

    public static void resetLevel() {

        initSizeDependentStuff(level.getWidth(), level.getHeight());

        level.reset();
        switchLevel(level, level.entrance, CharsList.emptyMobList);
    }

    public static String tip(Level _level) {
        if (_level instanceof DeadEndLevel) {

            return Game.getVar(R.string.Dungeon_DeadEnd);

        } else {
            String[] tips = Game.getVars(R.array.Dungeon_Tips);
            int index = depth - 1;

            if (index == -1) {
                return "Welcome to test level";
            }

            if (index < tips.length) {
                return tips[index];
            } else {
                return Game.getVar(R.string.Dungeon_NoTips);
            }
        }
    }

    public static boolean shopOnLevel(Level level) {
        if (level instanceof NecroLevel || level instanceof IceCavesLevel) {
            return false;
        } else {
            return depth == 6 || depth == 11 || depth == 16 || depth == 27;
        }

    }

    public static boolean bossLevel() {
        return Dungeon.level != null && Dungeon.level.isBossLevel();
    }

    public static void switchLevel(@NotNull final Level level, int pos, Collection<Mob> followers) {

        EventCollector.setSessionData("level",level.levelId);

        nightMode =new GregorianCalendar().get(Calendar.HOUR_OF_DAY) < 7;

        Actor.init(level);

        Actor respawner = level.respawner();
        if (respawner != null) {
            Actor.add(respawner);
        }

        hero.levelId = level.levelId;

        if (level.cellValid(pos)) {
            hero.setPos(pos);
        } else if(level.cellValid(level.entrance)){
            hero.setPos(level.entrance);
        } else {
            hero.setPos(level.getRandomTerrainCell(Terrain.EMPTY));
        }

        for(Mob mob : followers) {
                int cell = level.getEmptyCellNextTo(hero.getPos());
                if (!level.cellValid(cell)) {
                    cell = hero.getPos();
                }
                mob.setPos(cell);

                mob.setEnemy(CharsList.DUMMY);
                mob.setState(MobAi.getStateByClass(Wandering.class));
                level.spawnMob(mob);
            }

        levelId = level.levelId;
        Dungeon.level = level;
    }

    public static boolean posNeeded() {
        int[] quota = {4, 2, 9, 4, 14, 6, 19, 8, 24, 9};
        return chance(quota, potionOfStrength);
    }

    public static boolean soeNeeded() {
        int[] quota = {5, 3, 10, 6, 15, 9, 20, 12, 25, 13};
        return chance(quota, scrollsOfUpgrade);
    }

    private static boolean chance(int[] quota, int number) {

        for (int i = 0; i < quota.length; i += 2) {
            int qDepth = quota[i];
            if (depth <= qDepth) {
                int qNumber = quota[i + 1];
                return Random.Float() < (float) (qNumber - number) / (qDepth - depth + 1);
            }
        }

        return false;
    }

    public static boolean asNeeded() {
        return Random.Int(12 * (1 + arcaneStyli)) < depth;
    }

    private static final String VERSION      = "version";
    private static final String HERO         = "hero";
    private static final String DEPTH        = "depth";
    private static final String LEVEL        = "level";
    private static final String POS          = "potionsOfStrength";
    private static final String SOU          = "scrollsOfEnhancement";
    private static final String AS           = "arcaneStyli";
    private static final String DV           = "dewVial";
    private static final String WT           = "transmutation";
    private static final String CHAPTERS     = "chapters";
    private static final String QUESTS       = "quests";
    private static final String BADGES       = "badges";
    private static final String SCRIPTS_DATA = "scripts_data";
    private static final String GAME_ID      = "game_id";
    private static final String MOVE_TIMEOUT = "move_timeout";
    private static final String LAST_USED_ID = "lastUsedId";
    private static final String MOD          = "mod";
    private static final String REALTIME     = "realtime";
    private static final String CHALLENGES   = "challenges";


    public static void gameOver() {
        SaveUtils.deleteSaveFromSlot(SaveUtils.getPrevSave(),heroClass);
        Dungeon.deleteGame(true);
    }

    public static void saveGame(String fileName) throws IOException {
        Bundle bundle = new Bundle();

        bundle.put(GAME_ID, gameId);
        bundle.put(VERSION, Game.version);
        bundle.put(HERO, hero);
        bundle.put(DEPTH, depth);

        bundle.put(POS, potionOfStrength);
        bundle.put(SOU, scrollsOfUpgrade);
        bundle.put(AS, arcaneStyli);
        bundle.put(DV, dewVial);
        bundle.put(WT, transmutation);

        bundle.put(REALTIME, realtime);
        bundle.put(CHALLENGES, getChallenges());

        int count = 0;
        int[] ids = new int[chapters.size()];
        for (Integer id : chapters) {
            ids[count++] = id;
        }
        bundle.put(CHAPTERS, ids);

        Bundle quests = new Bundle();
        Ghost.Quest.storeInBundle(quests);
        WandMaker.Quest.storeInBundle(quests);
        Blacksmith.Quest.storeInBundle(quests);
        Imp.Quest.storeInBundle(quests);
        AzuterronNPC.Quest.storeInBundle(quests);
        ScarecrowNPC.Quest.storeInBundle(quests);
        CagedKobold.Quest.storeInBundle(quests);
        PlagueDoctorNPC.Quest.storeInBundle(quests);
        bundle.put(QUESTS, quests);

        Room.storeRoomsInBundle(bundle);

        Statistics.storeInBundle(bundle);
        Journal.storeInBundle(bundle);
        Logbook.storeInBundle(bundle);

        Scroll.save(bundle);
        Potion.save(bundle);
        Wand.save(bundle);
        Ring.save(bundle);
        QuickSlot.save(bundle);

        Bundle badges = new Bundle();
        Badges.saveLocal(badges);
        bundle.put(BADGES, badges);

        bundle.put(MOVE_TIMEOUT, moveTimeoutIndex);

        bundle.put(SCRIPTS_DATA,
                LuaEngine.getEngine().require(LuaEngine.SCRIPTS_LIB_STORAGE).get("serializeGameData").call().checkjstring());

        bundle.put(LAST_USED_ID, EntityIdSource.getNextId());
        bundle.put(MOD, ModdingMode.activeMod());

        OutputStream output = FileSystem.getOutputStream(fileName);
        Bundle.write(bundle, output);
        output.close();
    }

    @SneakyThrows
    private static void saveLevel(String saveTo, Level level) {
        Bundle bundle = new Bundle();
        bundle.put(LEVEL, level);

        bundle.put(SCRIPTS_DATA,
                LuaEngine.getEngine().require(LuaEngine.SCRIPTS_LIB_STORAGE).get("serializeLevelData").call().checkjstring());


        OutputStream output = FileSystem.getOutputStream(saveTo);
        Bundle.write(bundle, output);
        output.close();
    }

    private static void saveAllImpl() {
        float MBytesAvailable = Util.getAvailableInternalMemorySize() / 1024f / 1024f;

        if (MBytesAvailable < 2) {
            EventCollector.logEvent("saveGame", "lowMemory");
            Game.toast("Low memory condition");
        }

        if (level != null && hero!= null && hero.isAlive()) {
            Level thisLevel = Dungeon.level;

            Actor.fixTime();
            try {
                SaveUtils.copySaveToSlot(SaveUtils.getPrevSave(), heroClass);

                Position current = currentPosition();

                String saveToLevel = getLevelSaveFile(current);
                String saveToGame = SaveUtils.gameFile(hero.getHeroClass());

                saveGame(saveToGame);
                saveLevel(saveToLevel, thisLevel);

                Badges.saveGlobal();
                Library.saveLibrary();

                SaveUtils.copySaveToSlot(SaveUtils.getAutoSave(), heroClass);
            } catch (IOException e) {
                Game.toast(Game.getVar(R.string.Dungeon_saveIoError) + "\n" + e.getLocalizedMessage());
                EventCollector.logException(new Exception("cannot write save", e));
            }

            GamesInProgress.set(hero.getHeroClass(), depth, hero.lvl());

        } else if (WndResurrect.instance != null) {

            WndResurrect.instance.hide();
            Hero.reallyDie(hero, WndResurrect.causeOfDeath);
        } else {
            EventCollector.logException(new Exception("spurious save"));
        }
    }

    @NotNull
    private static String getLevelSaveFile(Position current) {
        return SaveUtils.depthFileForSave(hero.getHeroClass(),
                DungeonGenerator.getLevelDepth(current.levelId),
                DungeonGenerator.getLevelKind(current.levelId),
                current.levelId);
    }

    public synchronized static void save(boolean force) {

        if (!force && SystemTime.now() - lastSaveTimestamp < 1000) {
            return;
        }

        lastSaveTimestamp = SystemTime.now();

        saveAllImpl();
    }


    public static void loadGame() throws IOException {
        loadGame(SaveUtils.gameFile(heroClass), true);
    }

    public static void loadGameForRankings(String fileName) throws IOException {
        loadGame(fileName, false);
    }

    private static void loadGameFromBundle(Bundle bundle, boolean fullLoad) {

        if(fullLoad &&
                !bundle.optString(MOD,ModdingMode.REMIXED).equals(ModdingMode.activeMod())) {
            EventCollector.logException(new Exception("loading save from another mod"));
        }

        Dungeon.gameId = bundle.optString(GAME_ID, Utils.UNKNOWN);
        EntityIdSource.setLastUsedId(bundle.optInt(LAST_USED_ID,1));

        Treasury.reset();

        Scroll.restore(bundle);
        Potion.restore(bundle);
        Wand.restore(bundle);
        Ring.restore(bundle);
        QuickSlot.restore(bundle);

        potionOfStrength = bundle.getInt(POS);
        scrollsOfUpgrade = bundle.getInt(SOU);
        arcaneStyli = bundle.getInt(AS);
        dewVial = bundle.getBoolean(DV);
        transmutation = bundle.getInt(WT);

        realtime = bundle.getBoolean(REALTIME);
        setChallenges(bundle.optInt(CHALLENGES,0));

        if (fullLoad) {
            chapters = new HashSet<>();
            int[] ids = bundle.getIntArray(CHAPTERS);
            for (int id : ids) {
                chapters.add(id);
            }

            Bundle quests = bundle.getBundle(QUESTS);
            if (!quests.isNull()) {
                Ghost.Quest.restoreFromBundle(quests);
                WandMaker.Quest.restoreFromBundle(quests);
                Blacksmith.Quest.restoreFromBundle(quests);
                Imp.Quest.restoreFromBundle(quests);
                AzuterronNPC.Quest.restoreFromBundle(quests);
                ScarecrowNPC.Quest.restoreFromBundle(quests);
                CagedKobold.Quest.restoreFromBundle(quests);
                PlagueDoctorNPC.Quest.restoreFromBundle(quests);
            } else {
                Ghost.Quest.reset();
                WandMaker.Quest.reset();
                Blacksmith.Quest.reset();
                Imp.Quest.reset();
                AzuterronNPC.Quest.reset();
                ScarecrowNPC.Quest.reset();
                CagedKobold.Quest.reset();
                PlagueDoctorNPC.Quest.reset();
            }

            Room.restoreRoomsFromBundle(bundle);
        }

        Bundle badges = bundle.getBundle(BADGES);
        if (!badges.isNull()) {
            Badges.loadLocal(badges);
        } else {
            Badges.reset();
        }

        String version = bundle.getString(VERSION);

        hero = (Hero) bundle.get(HERO);
        if(hero==null) {
            throw new TrackedRuntimeException("no hero in bundle");
        }

        depth = bundle.getInt(DEPTH);

        Statistics.restoreFromBundle(bundle);
        Journal.restoreFromBundle(bundle);
        Logbook.restoreFromBundle(bundle);
        LuaEngine.getEngine().require(LuaEngine.SCRIPTS_LIB_STORAGE).get("deserializeGameData").call(bundle.getString(SCRIPTS_DATA));

        moveTimeoutIndex = RemixedDungeon.limitTimeoutIndex(bundle.optInt(MOVE_TIMEOUT, Integer.MAX_VALUE));
    }

    private static void loadGame(String fileName, boolean fullLoad) throws IOException {
        try {
            loading.incrementAndGet();
            Bundle bundle = gameBundle(fileName);
            loadGameFromBundle(bundle, fullLoad);
        }
        finally {
            loading.decrementAndGet();
        }
    }

    @NotNull
    @SneakyThrows
    public static Level loadLevel(Position next) {
        try {
            loading.incrementAndGet();
            levelId = next.levelId;
            Dungeon.level = null;
            DungeonGenerator.loadingLevel(next);

            String loadFrom = SaveUtils.depthFileForLoad(heroClass,
                    DungeonGenerator.getLevelDepth(levelId),
                    DungeonGenerator.getLevelKind(levelId),
                    levelId);

            GLog.toFile("loading level: %s", loadFrom);


            if(DungeonGenerator.isStatic(levelId)) {
                return newLevel(next);
            }

            if(!FileSystem.getFile(loadFrom).exists()) {
                return newLevel(next);
            }

            try(InputStream input =  new FileInputStream(FileSystem.getFile(loadFrom))) {
                Bundle bundle = Bundle.read(input);

                Level level = (Level) bundle.get("level");
                LuaEngine.getEngine().require(LuaEngine.SCRIPTS_LIB_STORAGE).get("deserializeLevelData").call(bundle.getString(SCRIPTS_DATA));

                if (level == null) {
                    level = newLevel(next);
                }

                level.levelId = next.levelId;
                initSizeDependentStuff(level.getWidth(), level.getHeight());

                return level;
            }
        } finally {
            loading.decrementAndGet();
        }
    }

    public static void deleteGame(boolean deleteLevels) {
        GLog.toFile("deleteGame");
        SaveUtils.deleteGameFile(heroClass);

        if (deleteLevels) {
            SaveUtils.deleteLevels(heroClass);
        }

        GamesInProgress.delete(heroClass);
    }

    public static Bundle gameBundle(String fileName) throws IOException {

        try(InputStream input = new FileInputStream(FileSystem.getFile(fileName))) {
            return Bundle.read(input);
        }
    }

    public static void preview(GamesInProgress.Info info, Bundle bundle) {
        info.depth = bundle.getInt(DEPTH);
        if (info.depth == -1) {
            info.depth = bundle.getInt("maxDepth"); // FIXME
        }
        Hero.preview(info, bundle.getBundle(HERO));
    }

    public static void fail(String desc) {
        if (hero.getBelongings().getItem(Ankh.class) == null) {
            Rankings.INSTANCE.submit(Rankings.gameOver.LOSE, desc);
        }
    }

    public static void win(String desc, gameOver kind) {
        if (getChallenges() != 0) {
            Badges.validateChampion();
        }
        Rankings.INSTANCE.submit(kind, desc);
    }


    public static void observe() {
        GameScene.observeRequest();
    }

    public static void observeImpl() {
        level.updateFieldOfView(hero.getControlTarget());
        System.arraycopy(level.fieldOfView, 0, visible, 0, visible.length);

        BArray.or(level.mapped, visible, level.mapped);

        GameScene.afterObserve();
    }

    private static void markActorsAsUnpassableIgnoreFov() {
        for (Char actor : Actor.chars.values()) {
            int pos = actor.getPos();
            passable[pos] = false;
        }
    }

    private static void markObjects(Char ch) {
        for (int i = 0; i < level.objects.size(); i++) {
            SparseArray<LevelObject> objectLayer = level.objects.valueAt(i);
            for (int j = 0; j < objectLayer.size(); j++) {
                LevelObject object = objectLayer.valueAt(j);
                int pos = object.getPos();
                if(!level.cellValid(pos)) {
                    EventCollector.logException("Invalid object "+object.getEntityKind() + "pos in layer "+ i);
                    level.remove(object);
                    continue;
                }
                if (object.nonPassable(ch)) {
                    passable[object.getPos()] = false;
                }
            }
        }
    }

    private static void markActorsAsUnpassable(Char ch, boolean[] visible) {
        for (Char actor : Actor.chars.values()) {
            int pos = actor.getPos();

            if(!level.cellValid(pos)) {
                GLog.debug("WTF?");
                return;
            }

            if (visible[pos]) {
                if (actor instanceof Mob) {
                    passable[pos] = passable[pos] && !level.avoid[pos] && actor.getOwnerId() == ch.getId();
                }
            }
        }
    }


    public static int findPath(@NotNull Hero ch, int to, boolean[] pass, boolean[] visible) {

        int from  = ch.getPos();

        if (level.adjacent(from, to)) {
            if (!(pass[to] || level.avoid[to])) {
                return Level.INVALID_CELL;
            }

            Char chr = Actor.findChar(to);

            if (chr instanceof Mob) {
                Mob mob = (Mob) chr;
                if (mob.isPet()) {
                    return to;
                }
            }
            return chr == null ? to : Level.INVALID_CELL;
        }

        if (ch.isFlying() || ch.hasBuff(Amok.class)) {
            BArray.or(pass, level.avoid, passable);
        } else {
            System.arraycopy(pass, 0, passable, 0, level.getLength());
        }

        markActorsAsUnpassable(ch, visible);

        markObjects(ch);

        return PathFinder.getStep(from, to, passable);
    }


    public static int findPath(@NotNull Char ch, int to, boolean[] pass) {

        int from = ch.getPos();

        if (level.adjacent(from, to)) {

            if (!(pass[to] || level.avoid[to])) {
                return Level.INVALID_CELL;
            }

            return Actor.findChar(to) == null ? to : Level.INVALID_CELL;
        }

        if (ch.isFlying() || ch.hasBuff(Amok.class)) {
            BArray.or(pass, level.avoid, passable);
        } else {
            System.arraycopy(pass, 0, passable, 0, level.getLength());
        }

        markActorsAsUnpassableIgnoreFov();

        markObjects(ch);

        return PathFinder.getStep(from, to, passable);

    }

    public static int flee(Char ch, int from, boolean[] pass) {

        int cur = ch.getPos();

        if (ch.isFlying()) {
            BArray.or(pass, level.avoid, passable);
        } else {
            System.arraycopy(pass, 0, passable, 0, level.getLength());
        }

        markActorsAsUnpassableIgnoreFov();
        markObjects(ch);

        passable[cur] = true;

        return PathFinder.getStepBack(cur, from, passable);
    }

    public static Position currentPosition() {
        return new Position(hero.levelId, hero.getPos());
    }

    private static int difficulty;

    public static void setDifficulty(int _difficulty) {
        difficulty = _difficulty;
        RemixedDungeon.setDifficulty(difficulty);
    }

    public static boolean isLoading() {
        return hero==null || level == null || loading.get()>0;
    }

    public static boolean realtime() {
        return realtime;
    }

    public static double moveTimeout() {
        return MOVE_TIMEOUTS[moveTimeoutIndex];
    }

    public static void saveCurrentLevel() {
        saveLevel(getLevelSaveFile(currentPosition()), Dungeon.level);
    }

    public static int getChallenges() {
        return challenges;
    }

    public static void setChallenges(int challenges) {
        EventCollector.setSessionData("challenges",String.valueOf(challenges));
        Dungeon.challenges = challenges;
    }

    public static boolean isPathVisible(int from, int to) {
        return visible[from] || visible[to];
    }

    @LuaInterface
    public static boolean isCellVisible(int cell) {
        return visible[cell];
    }

}
