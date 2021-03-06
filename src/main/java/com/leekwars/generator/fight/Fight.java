package com.leekwars.generator.fight;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leekwars.generator.Generator;
import com.leekwars.generator.ErrorManager;
import com.leekwars.generator.Log;
import com.leekwars.generator.attack.Attack;
import com.leekwars.generator.attack.EffectParameters;
import com.leekwars.generator.attack.chips.Chip;
import com.leekwars.generator.attack.chips.Chips;
import com.leekwars.generator.attack.effect.Effect;
import com.leekwars.generator.attack.weapons.Weapon;
import com.leekwars.generator.fight.action.Action;
import com.leekwars.generator.fight.action.ActionEndTurn;
import com.leekwars.generator.fight.action.ActionEntityDie;
import com.leekwars.generator.fight.action.ActionEntityTurn;
import com.leekwars.generator.fight.action.ActionInvocation;
import com.leekwars.generator.fight.action.ActionLoseMP;
import com.leekwars.generator.fight.action.ActionLoseTP;
import com.leekwars.generator.fight.action.ActionMove;
import com.leekwars.generator.fight.action.ActionNewTurn;
import com.leekwars.generator.fight.action.ActionResurrect;
import com.leekwars.generator.fight.action.ActionStartFight;
import com.leekwars.generator.fight.action.ActionUseChip;
import com.leekwars.generator.fight.action.ActionUseWeapon;
import com.leekwars.generator.fight.action.Actions;
import com.leekwars.generator.fight.entity.Bulb;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.fight.statistics.FightStatistics;
import com.leekwars.generator.leek.Leek;
import com.leekwars.generator.maps.Cell;
import com.leekwars.generator.maps.Map;
import com.leekwars.generator.maps.Pathfinding;

import leekscript.runner.values.FunctionLeekValue;

public class Fight {

	public final static String TAG = Fight.class.getSimpleName();

	// Maximum number of turns
	public final static int MAX_TURNS = 64;

	// Fight full types (type + context)
	public static final int TYPE_SOLO_GARDEN = 1;
	public static final int TYPE_SOLO_TEST = 2;
	public static final int TYPE_NORMAL_WHAT = 3;
	public static final int TYPE_TEAM_GARDEN = 4;
	public static final int TYPE_SOLO_CHALLENGE = 5;
	public static final int TYPE_FARMER_GARDEN = 6;
	public static final int TYPE_SOLO_TOURNAMENT = 7;
	public static final int TYPE_TEAM_TEST = 8;
	public static final int TYPE_FARMER_TOURNAMENT = 9;
	public static final int TYPE_TEAM_TOURNAMENT = 10;
	public static final int TYPE_FARMER_CHALLENGE = 11;
	public static final int TYPE_FARMER_TEST = 12;
	public static final int FULL_TYPE_BATTLE_ROYALE = 15;

	// Fight contexts
	public final static int CONTEXT_TEST = 0;
	public final static int CONTEXT_CHALLENGE = 1;
	public final static int CONTEXT_GARDEN = 2;
	public final static int CONTEXT_TOURNAMENT = 3;
	public final static int CONTEXT_BATTLE_ROYALE = 5;

	// Fight types
	public final static int TYPE_SOLO = 0;
	public final static int TYPE_FARMER = 1;
	public final static int TYPE_TEAM = 2;
	public final static int TYPE_BATTLE_ROYALE = 3;

	// Fight states
	public final static int STATE_INIT = 0;
	public final static int STATE_RUNNING = 1;
	public final static int STATE_FINISHED = 2;

	// Flags
	public final static int FLAG_STATIC = 1;
	public final static int FLAG_PERFECT = 2;

	// Summon limit
	public final static int SUMMON_LIMIT = 8;

	public final static int MAX_LOG_COUNT = 5000;

	private final List<Team> teams;
	private final List<Entity> initialOrder;

	private int mNextEntityId = 0;
	private int mWinteam = -1;

	private final java.util.Map<Integer, Entity> mEntities;
	private final TreeMap<Integer, Entity> mEntitiesById;

	private int mId;
	public int mState;
	private Order order;
	private final int fullType;
	private int mStartFarmer = -1;
	private int max_turns = MAX_TURNS;

	private Map map;
	private final Actions actions;

	private String mLeekDatas = "";

	private int context;
	private int type;

	JSONObject custom_map = null;

	public FightStatistics statistics;

	public Fight() {

		teams = new ArrayList<Team>();
		initialOrder = new ArrayList<Entity>();

		mState = Fight.STATE_INIT;

		actions = new Actions();

		mEntitiesById = new TreeMap<Integer, Entity>();
		mEntities = new TreeMap<Integer, Entity>();

		statistics = new FightStatistics();

		type = TYPE_SOLO;
		context = CONTEXT_GARDEN;
		fullType = TYPE_SOLO_GARDEN;
	}

	public int getFightType() {
		return type;
	}

	public int getFightContext() {
		return context;
	}

	public void addFlag(int team, int flag) {
		teams.get(team).addFlag(flag);
	}

	public HashSet<Integer> getFlags(int team) {
		return teams.get(team).getFlags();
	}

	public int getWinner() {
		return mWinteam;
	}

	public void setTeamID(int team, int id) {
		if (team < teams.size()) {
			teams.get(team).setID(id);
		}
	}

	public java.util.Map<Integer, Entity> getEntities() {
		return mEntities;
	}

	public void setStartFarmer(int startFarmer) {
		mStartFarmer = startFarmer;
	}

	public int getStartFarmer() {
		return mStartFarmer;
	}

	public int getTeamID(int team) {
		if (team < teams.size()) {
			return teams.get(team).getID();
		}
		return -1;
	}

	public int getId() {
		return mId;
	}

	public String getLeekDatas() {
		return mLeekDatas;
	}

	public void log(Action log) {
		actions.log(log);
	}

	public Map getMap() {
		return map;
	}

	public void setCustomMap(JSONObject map) {
		custom_map = map;
	}

	/*
	 * Add Entity from json fight 'team' is whether the entity is in 'leeks1' or
	 * 'leeks2'
	 */
	public void addEntity(int t, Entity entity) {

		if (this.mState != Fight.STATE_INIT) {
			return;
		}
		if (entity == null || t < 0) {
			return;
		}

		int team = t;
		if (type == Fight.TYPE_BATTLE_ROYALE) {
			team = teams.size();
		}

		while (teams.size() < team + 1) {
			teams.add(new Team());
		}

		entity.setTeam(team);

		teams.get(team).addEntity(entity);

		entity.setFight(this, getNextEntityId());
		mEntitiesById.put(entity.getId(), entity);
		mEntities.put(entity.getFId(), entity);
	}

	public Entity getEntityById(int id) {
		return mEntitiesById.get(id);
	}

	public List<Entity> getEnemiesEntities(int team) {
		return getEnemiesEntities(team, false);
	}

	public List<Entity> getEnemiesEntities(int team, boolean get_deads) {

		List<Entity> enemies = new ArrayList<Entity>();
		for (int t = 0; t < teams.size(); ++t) {
			if (t != team) {
				enemies.addAll(getTeamEntities(t, get_deads));
			}
		}
		return enemies;
	}

	public List<Entity> getTeamEntities(int team) {
		return getTeamEntities(team, false);
	}

	public List<Entity> getTeamEntities(int team, boolean dead) {

		List<Entity> leeks = new ArrayList<Entity>();

		if (team < teams.size()) {
			for (Entity e : teams.get(team).getEntities()) {
				if (dead || !e.isDead())
					leeks.add(e);
			}
		}
		return leeks;
	}

	public List<Entity> getAllEntities(boolean get_deads) {

		List<Entity> leeks = new ArrayList<Entity>();

		for (Team t : teams) {
			for (Entity e : t.getEntities()) {
				if (get_deads || !e.isDead()) {
					leeks.add(e);
				}
			}
		}
		return leeks;
	}

	public Entity getEntity(int id) {
		Entity l = mEntities.get(id);
		return l;
	}

	private boolean canStartFight() {
		if (teams.size() < 2) return false;
		return true;
	}

	public void startFight() throws FightException {

		initFight();

		// Check all entities characteristics
		for (Entity entity : mEntities.values()) {
			statistics.checkCharacteristics(entity);
		}

		mState = Fight.STATE_RUNNING;

		int count_errors = 0;

		Log.i(TAG, "Turn 1");

		// On lance les tours
		while (order.getTurn() <= max_turns && mState == Fight.STATE_RUNNING) {

			try {
				startTurn();
			} catch (Exception e) {
				ErrorManager.exceptionFight(e, mId);
				count_errors++;
				if (count_errors > 50) {
					break;
				}
			}
			if (order.current() == null) {
				finishFight();
				break;
			}
		}
		// Si match nul
		if (order.getTurn() == Fight.MAX_TURNS + 1) {
			finishFight();
		}

		// On supprime toutes les invocations
		List<Entity> entities = getAllEntities(true);
		for (Entity e : entities) {
			if (e.isSummon())
				removeInvocation(e, true);
		}

		mWinteam = -1;

		int alive = 0;
		for (int t = 0; t < teams.size(); ++t) {
			if (!teams.get(t).isDead()) {
				alive++;
				mWinteam = t;
			}
		}
		if (alive != 1) {
			mWinteam = -1;
		}
		statistics.endFight(mEntities.values());
	}

	public void initFight() throws FightException {

		if (this.mState != Fight.STATE_INIT) {
			return;
		}

		if (teams.size() > 0 && (teams.get(0).size() == 0 || teams.get(1).size() == 0)) {
			if (Fight.getFightContext(fullType) == Fight.CONTEXT_TOURNAMENT) {
				// Si c'est un tournoi faut un gagnant
				if (teams.get(0).size() == 0) {
					mWinteam = 1;
				} else {
					mWinteam = 0;
				}
			}
			throw new FightException(FightException.NOT_ENOUGHT_PLAYERS);
		}

		if (!canStartFight()) {
			throw new FightException(FightException.CANT_START_FIGHT);
		}

		// Create level/skin list
		JSONObject list = new JSONObject();

		for (Entity l : mEntities.values()) {

			JSONArray data = new JSONArray();
			data.add(l.getLevel());
			data.add(l.getSkin());

			if (l.getHat() > 0) {
				data.add(l.getHat());
			} else {
				data.add(null);
			}
			list.put(String.valueOf(l.getId()), data);
		}
		mLeekDatas = list.toJSONString();

		int obstacle_count = Generator.getRandom().getInt(40, 60);

		this.map = Map.generateMap(context, 18, 18, obstacle_count, teams, custom_map);

		// Initialize positions and game order
		StartOrder bootorder = new StartOrder();
		this.order = new Order();

		for (Team t : teams) {
			for (Entity e : t.getEntities()) {
				bootorder.addEntity(e);
			}
		}

		try {
			for (Entity e : bootorder.compute()) {
				this.order.addEntity(e);
				actions.addEntity(e);
				initialOrder.add(e);
			}
		} catch (Exception e) {
			ErrorManager.exceptionFight(e, mId);
		}

		// Initialize statistics
		statistics.initializeEntities(mEntities.values());

		// On ajoute la map
		actions.addMap(map);

		// Cooldowns initiaux
		for (Entry<Integer, Chip> entry : Chips.getTemplates().entrySet()) {
			Chip chip = entry.getValue();
			if (chip.getInitialCooldown() > 0) {
				for (Team t : teams) {
					for (Entity entity : t.getEntities()) {
						addCooldown(entity, chip, chip.getInitialCooldown() + 1);
					}
				}
			}
		}

		// Puis on ajoute le startfight
		actions.log(new ActionStartFight(teams.get(0).size(), teams.get(1).size()));
	}

	public void finishFight() {
		mState = Fight.STATE_FINISHED;
	}

	public void startTurn() {

		Entity current = order.current();
		if (current == null) {
			return;
		}

		ActionEntityTurn lt;
		actions.log(lt = new ActionEntityTurn(current));
		Log.i(TAG, "Start turn of " + current.getName());

		current.applyCoolDown();
		current.startTurn();

		lt.setPM(order.current().getMP());
		lt.setTP(order.current().getTP());

		if (!current.isDead() && order.current().getUsedLeekIA() != null) {

			order.current().getUsedLeekIA().runTurn();

			if (order.current() != null) {
				order.current().endTurn();
			}
		}
		endTurn();
	}

	public void onPlayerDie(Entity entity, Entity killer) {

		if (entity.isDead())
			return;

		statistics.addKills(1);
		entity.setDead(true);
		order.removeEntity(entity);
		if (entity.getCell() != null) {
			entity.getCell().setPlayer(null);
		}
		entity.setCell(null);

		actions.log(new ActionEntityDie(entity, killer));
	}

	/*
	 * Determines if the fight is over : only one team is alive
	 */
	public boolean isFinished() {

		int teamsAlive = 0;
		for (Team team : teams) {
			if (team.isAlive()) {
				teamsAlive++;
				if (teamsAlive > 1) {
					return false;
				}
			}
		}
		return true;
	}

	public void endTurn() {

		if (isFinished()) {
			mState = Fight.STATE_FINISHED;
		} else {

			actions.log(new ActionEndTurn(order.current()));

			if (order.next()) {

				if (order.getTurn() <= Fight.MAX_TURNS) {
					actions.log(new ActionNewTurn(order.getTurn()));
					Log.i(TAG, "Turn " + order.getTurn());
				}

				for (Team t : teams) {
					t.applyCoolDown();
				}
			}
		}
	}

	// Attaques; Déplacements...
	public int useWeapon(Entity launcher, Cell target) {

		if (order.current() != launcher || launcher.getWeapon() == null) {
			return Attack.USE_INVALID_TARGET;
		}

		Weapon weapon = launcher.getWeapon();
		if (weapon.getCost() > launcher.getTP()) {
			return Attack.USE_NOT_ENOUGH_TP;
		}

		if (!Pathfinding.canUseAttack(launcher.getCell(), target, weapon.getAttack())) {
			return Attack.USE_INVALID_POSITION;
		}

		boolean critical = generateCritical(launcher);
		int result = critical ? Attack.USE_CRITICAL : Attack.USE_SUCCESS;

		ActionUseWeapon log_use = new ActionUseWeapon(launcher, target, weapon, result);
		actions.log(log_use);
		List<Entity> target_leeks = weapon.getAttack().applyOnCell(this, launcher, target, critical);
		statistics.weaponUsed(launcher, weapon, target_leeks);
		log_use.setEntities(target_leeks);

		launcher.useTP(weapon.getCost());
		actions.log(new ActionLoseTP(launcher, weapon.getCost()));
		if (critical) {
			statistics.addCriticalHits(1);
		}

		return result;
	}

	public int useChip(Entity caster, Cell target, Chip template) {

		if (order.current() != caster) {
			return Attack.USE_INVALID_TARGET;
		}
		if (template.getCost() > caster.getTP()) {
			return Attack.USE_NOT_ENOUGH_TP;
		}
		if (!Pathfinding.canUseAttack(caster.getCell(), target, template.getAttack())) {
			return Attack.USE_INVALID_POSITION;
		}
		if (hasCooldown(caster, template)) {
			return Attack.USE_INVALID_COOLDOWN;
		}
		if (template.getAttack().getEffectParametersByType(Effect.TYPE_SUMMON) != null) {
			return summonEntity(caster, target, template, null);
		}

		for (EffectParameters parameters : template.getAttack().getEffects()) {
			// Si c'est une téléportation on ajoute une petite vérification
			if (parameters.getId() == Effect.TYPE_TELEPORT && !target.available()) {
				return Attack.USE_INVALID_TARGET;
			}
			// Impossible d'inverser une entité statique
			if (parameters.getId() == Effect.TYPE_PERMUTATION && target.getPlayer() != null && target.getPlayer().isStatic()) {
				return Attack.USE_INVALID_TARGET;
			}
		}

		boolean critical = generateCritical(caster);
		int result = critical ? Attack.USE_CRITICAL : Attack.USE_SUCCESS;

		ActionUseChip log = new ActionUseChip(caster, target, template, result);
		actions.log(log);
		List<Entity> targets = template.getAttack().applyOnCell(this, caster, target, critical);
		log.setEntities(targets);
		statistics.chipUsed(caster, template, targets);

		if (template.getCooldown() != 0) {
			addCooldown(caster, template);
		}

		caster.useTP(template.getCost());
		actions.log(new ActionLoseTP(caster, template.getCost()));
		if (critical) {
			statistics.addCriticalHits(1);
		}

		return result;
	}

	public int moveEntity(Entity entity, List<Cell> path) {

		if (entity.isStatic()) return 0; // Static entity cannot move.

		int size = path.size();
		if (size == 0) {
			return 0;
		}
		if (size > entity.getMP()) {
			return 0;
		}
		actions.log(new ActionMove(entity, path));
		actions.log(new ActionLoseMP(entity, size));

		statistics.entityMove(entity, path);
		entity.useMP(size);
		entity.setHasMoved(true);
		entity.getCell().setPlayer(null);
		entity.setCell(path.get(path.size() - 1));
		entity.getCell().setPlayer(entity);

		return path.size();
	}

	public int summonEntity(Entity caster, Cell target, Chip template, FunctionLeekValue value) {

		if (order.current() != caster) {
			return -1;
		}
		if (template.getCost() > caster.getTP()) {
			return -2;
		}
		if (!Pathfinding.canUseAttack(caster.getCell(), target, template.getAttack())) {
			return -4;
		}
		if (hasCooldown(caster, template)) {
			return -3;
		}
		EffectParameters params = template.getAttack().getEffectParametersByType(Effect.TYPE_SUMMON);
		if (params == null || !target.available()) {
			return -3;
		}
		if (teams.get(caster.getTeam()).getSummonCount() >= SUMMON_LIMIT) {
			return -5;
		}

		int result = Attack.USE_SUCCESS;

		ActionUseChip log = new ActionUseChip(caster, target, template, result);
		actions.log(log);

		// On invoque
		Entity summon = createSummon(caster, (int) params.getValue1(), target, value, template.getLevel());
		statistics.summon(caster, summon);

		if (template.getCooldown() != 0) {
			addCooldown(caster, template);
		}

		caster.useTP(template.getCost());
		statistics.chipUsed(caster, template, new ArrayList<Entity>());
		actions.log(new ActionLoseTP(caster, template.getCost()));

		return result;
	}

	public int resurrectEntity(Entity caster, Cell target, Chip template, Entity target_entity) {

		if (order.current() != caster) {
			return Attack.USE_INVALID_TARGET;
		}
		if (template.getCost() > caster.getTP()) {
			return Attack.USE_NOT_ENOUGH_TP;
		}
		if (!Pathfinding.canUseAttack(caster.getCell(), target, template.getAttack())) {
			return Attack.USE_INVALID_POSITION;
		}
		if (hasCooldown(caster, template)) {
			return Attack.USE_INVALID_COOLDOWN;
		}
		EffectParameters params = template.getAttack().getEffectParametersByType(Effect.TYPE_RESURRECT);
		if (params == null || !target.available() || !target_entity.isDead() || target_entity.getTeam() != caster.getTeam()) {
			return Attack.USE_INVALID_TARGET;
		}

		if (target_entity.isSummon()) {
			// It's a summon
			if (teams.get(target_entity.getTeam()).getSummonCount() >= SUMMON_LIMIT) {
				return Attack.USE_TOO_MANY_SUMMONS;
			}
		}

		int result = Attack.USE_SUCCESS;

		ActionUseChip log = new ActionUseChip(caster, target, template, result);
		actions.log(log);

		// Resurrect
		resurrect(caster, target_entity, target);
		statistics.addResurrects(1);

		if (template.getCooldown() != 0) {
			addCooldown(caster, template);
		}

		caster.useTP(template.getCost());
		statistics.chipUsed(caster, template, new ArrayList<Entity>());
		actions.log(new ActionLoseTP(caster, template.getCost()));

		return result;
	}

	public boolean generateCritical(Entity caster) {
		return Generator.getRandom().getDouble() < ((double) caster.getAgility() / 1000);
	}

	public Bulb createSummon(Entity owner, int type, Cell target, FunctionLeekValue ai, int level) {

		int fid = getNextEntityId();
		Bulb invoc = Bulb.create(owner, ai, -fid, type, level);
		invoc.setFight(this, fid);

		int team = owner.getTeam();

		invoc.setTeam(team);
		teams.get(team).addEntity(invoc);

		// On ajoute dans les tableaux
		mEntities.put(invoc.getFId(), invoc);
		mEntitiesById.put(invoc.getId(), invoc);

		// On ajoute dans l'ordre de jeu
		order.addSummon(owner, invoc);

		// On met la cellule
		target.setPlayer(invoc);

		// On l'ajoute dans les infos du combat
		actions.addEntity(invoc);

		// On balance l'action
		actions.log(new ActionInvocation(invoc));

		return invoc;
	}

	public void removeInvocation(Entity invoc, boolean force) {

		// Mort d'une invocation, on la retire des listes
		teams.get(invoc.getTeam()).removeEntity(invoc);

		if (force) {
			mEntities.remove(invoc.getFId());
			mEntitiesById.remove(invoc.getId());
		}
	}

	public void resurrect(Entity owner, Entity entity, Cell cell) {

		Entity next = null;
		boolean start = false;
		for (Entity e : initialOrder) {
			if (e == entity) {
				start = true;
				continue;
			}
			if (!start) {
				continue;
			}
			if (e.isDead()) {
				continue;
			}
			next = e;
			break;
		}
		if (next == null) {
			order.addEntity(entity);
		} else {
			order.addEntity(order.getEntityTurnOrder(next) - 1, entity);
		}
		entity.resurrect(owner);

		// On met la cellule
		cell.setPlayer(entity);

		// On balance l'action
		actions.log(new ActionResurrect(owner, entity));
	}

	public int getTurn() {
		return order.getTurn();
	}

	public Order getOrder() {
		return order;
	}

	public Actions getActions() {
		return actions;
	}

	public int getFullType() {
		return fullType;
	}

	public void setId(int f) {
		mId = f;
	}

	public java.util.Map<Integer, Leek> getLeeks() {
		java.util.Map<Integer, Leek> retour = new TreeMap<Integer, Leek>();
		for (Entry<Integer, Entity> e : mEntities.entrySet()) {
			Leek l = e.getValue().getLeek();
			if (l != null)
				retour.put(e.getKey(), l);
		}
		return retour;
	}

	public int getNextEntityId() {
		int id = mNextEntityId;
		mNextEntityId++;
		return id;
	}

	// Add a cooldown for a chip
	public void addCooldown(Entity entity, Chip chip) {
		addCooldown(entity, chip, chip.getCooldown());
	}

	public void addCooldown(Entity entity, Chip chip, int cooldown) {
		if (chip == null) {
			return;
		}
		if (chip.isTeamCooldown()) {
			teams.get(entity.getTeam()).addCooldown(chip, cooldown);
		} else {
			entity.addCooldown(chip, cooldown);
		}
	}

	// Chip has cooldown?
	public boolean hasCooldown(Entity entity, Chip chip) {
		if (chip == null) {
			return false;
		}
		if (chip.isTeamCooldown()) {
			return teams.get(entity.getTeam()).hasCooldown(chip.getId());
		} else {
			return entity.hasCooldown(chip.getId());
		}
	}

	// Get current cooldown of a chip
	public int getCooldown(Entity entity, Chip chip) {
		if (chip == null) {
			return 0;
		}
		if (chip.isTeamCooldown()) {
			return teams.get(entity.getTeam()).getCooldown(chip.getId());
		} else {
			return entity.getCooldown(chip.getId());
		}
	}

	public int getType() {
		return type;
	}

	public int getContext() {
		return context;
	}

	public static int getFightContext(int type) {
		if (type == TYPE_SOLO_GARDEN || type == TYPE_TEAM_GARDEN || type == TYPE_FARMER_GARDEN) {
			return CONTEXT_GARDEN;
		} else if (type == TYPE_SOLO_TEST || type == TYPE_TEAM_TEST || type == TYPE_FARMER_TEST) {
			return CONTEXT_TEST;
		} else if (type == TYPE_TEAM_TOURNAMENT || type == TYPE_SOLO_TOURNAMENT || type == TYPE_FARMER_TOURNAMENT) {
			return CONTEXT_TOURNAMENT;
		} else if (type == FULL_TYPE_BATTLE_ROYALE) {
			return CONTEXT_BATTLE_ROYALE;
		}
		return CONTEXT_CHALLENGE;
	}

	public static int getFightType(int type) {
		if (type == TYPE_SOLO_GARDEN || type == TYPE_SOLO_CHALLENGE || type == TYPE_SOLO_TOURNAMENT || type == TYPE_SOLO_TEST) {
			return Fight.TYPE_SOLO;
		} else if (type == TYPE_FARMER_GARDEN || type == TYPE_FARMER_TOURNAMENT || type == TYPE_FARMER_CHALLENGE || type == TYPE_FARMER_TEST) {
			return Fight.TYPE_FARMER;
		} else if (type == FULL_TYPE_BATTLE_ROYALE) {
			return Fight.TYPE_BATTLE_ROYALE;
		}
		return Fight.TYPE_TEAM;
	}

	public static boolean isTeamAIFight(int type) {
		return getFightType(type) != TYPE_SOLO;
	}

	public static boolean isTestFight(int type) {
		return getFightContext(type) == CONTEXT_TEST;
	}

	public static boolean isChallenge(int type) {
		return getFightContext(type) == CONTEXT_CHALLENGE;
	}

	public List<Team> getTeams() {
		return teams;
	}

	public void setMaxTurns(int max_turns) {
		this.max_turns = max_turns;
	}

	public JSONObject getDeadReport() {
		JSONObject dead = new JSONObject();
		for (Entity entity : mEntities.values()) {
			dead.put(String.valueOf(entity.getId()), entity.isDead());
		}
		return dead;
	}

	public void setContext(int context) {
		this.context = context;
	}
	public void setType(int type) {
		this.type = type;
	}

	private double getMaxDeadRatio() {
		double max = 0;
		for (Team team : teams) {
			double ratio = team.getDeadRatio();
			if (ratio > max) max = ratio;
		}
		return max;
	}
	private double getMaxLifeRatio() {
		double max = 0;
		for (Team team : teams) {
			double ratio = 1 - team.getLifeRatio();
			if (ratio > max) max = ratio;
		}
		return max;
	}

	public double getProgress() {
		if (this.order == null) return 0;

		int entityCount = this.order.getEntities().size();

		double d = getMaxDeadRatio();
		double t = Math.pow((double) (this.getTurn() * entityCount + this.order.getPosition()) / (MAX_TURNS * entityCount), 0.5);
		double l = getMaxLifeRatio();

		return Math.max(t, Math.max(d, l));
	}
}
