package com.leekwars.game.attack.effect;

import java.util.List;

import leekscript.runner.AI;
import leekscript.runner.LeekValueManager;
import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.ArrayLeekValue;

import com.leekwars.game.fight.Fight;
import com.leekwars.game.fight.action.ActionAddEffect;
import com.leekwars.game.fight.entity.Entity;
import com.leekwars.game.leek.Stats;

public abstract class Effect {

	// Effect type constants
	public final static int TYPE_DAMAGE = 1;
	public final static int TYPE_HEAL = 2;
	public final static int TYPE_BUFF_STRENGTH = 3;
	public final static int TYPE_BUFF_AGILITY = 4;
	public final static int TYPE_RELATIVE_SHIELD = 5;
	public final static int TYPE_ABSOLUTE_SHIELD = 6;
	public final static int TYPE_BUFF_MP = 7;
	public final static int TYPE_BUFF_TP = 8;
	public final static int TYPE_DEBUFF = 9;
	public final static int TYPE_TELEPORT = 10;
	public final static int TYPE_PERMUTATION = 11;
	public final static int TYPE_VITALITY = 12;
	public final static int TYPE_POISON = 13;
	public final static int TYPE_SUMMON = 14;
	public final static int TYPE_RESURRECT = 15;
	public final static int TYPE_KILL = 16;
	public final static int TYPE_SHACKLE_MP = 17;
	public final static int TYPE_SHACKLE_TP = 18;
	public final static int TYPE_SHACKLE_STRENGTH = 19;
	public final static int TYPE_DAMAGE_RETURN = 20;
	public final static int TYPE_BUFF_RESISTANCE = 21;
	public final static int TYPE_BUFF_WISDOM = 22;
	public final static int TYPE_ANTIDOTE = 23;
	public final static int TYPE_SHACKLE_MAGIC = 24;
	public final static int TYPE_AFTEREFFECT = 25;
	public final static int TYPE_VULNERABILITY = 26;

	// Target filters constants
	public final static int TARGET_ENEMIES = 1; // Enemies
	public final static int TARGET_ALLIES = 2; // Allies
	public final static int TARGET_CASTER = 4; // Caster
	public final static int TARGET_NON_SUMMONS = 8; // Non-summons
	public final static int TARGET_SUMMONS = 16; // Summons

	// Power in case of critical hit
	private static final double CRITICAL_POWER = 1.4;

	// Array of effect classes
	private final static Class<?>[] effects = { EffectDamage.class, EffectHeal.class, EffectBuffStrength.class, EffectBuffAgility.class, EffectRelativeShield.class, EffectAbsoluteShield.class,
			EffectBuffMP.class, EffectBuffTP.class, EffectDebuff.class, EffectTeleport.class, EffectPermutation.class, EffectVitality.class, EffectPoison.class, EffectSummon.class,
			EffectResurrect.class, EffectKill.class, EffectShackleMP.class, EffectShackleTP.class, EffectShackleStrength.class, EffectDamageReturn.class, EffectBuffResistance.class,
			EffectBuffWisdom.class, EffectAntidote.class, EffectShackleMagic.class, EffectAftereffect.class, EffectVulnerability.class };

	// Effect characteristics
	protected int id;
	protected int turns = 0;
	protected double power = 1.0;
	protected double value1;
	protected double value2;
	protected boolean critical = false;
	protected double criticalPower = 1.0;
	protected Entity caster;
	protected Entity target;
	protected int attackType;
	protected int attackID;
	protected double jet;
	protected Stats stats = new Stats();
	protected int logID = 0;
	protected double erosionRate;
	public int value = 0;

	public static void createEffect(Fight fight, int id, int turns, double power, double value1, double value2, boolean critical, Entity target, Entity caster, int attack_type, int attack_id,
			double jet) {

		// Invalid effect id
		if (id < 0 || id > effects.length) {
			return;
		}

		// Create the effect
		Effect effect;
		try {
			effect = (Effect) effects[id - 1].newInstance();
		} catch (Exception e) {
			return;
		}
		effect.id = id;
		effect.turns = turns;
		effect.power = power;
		effect.value1 = value1;
		effect.value2 = value2;
		effect.critical = critical;
		effect.criticalPower = critical ? CRITICAL_POWER : 1.0;
		effect.caster = caster;
		effect.target = target;
		effect.attackType = attack_type;
		effect.attackID = attack_id;
		effect.jet = jet;
		effect.erosionRate = id == TYPE_POISON ? 0.10 : 0.05;
		if (critical) effect.erosionRate += 0.10;

		// Add effect to the target and the caster
		if (effect.getTurns() > 0) {

			boolean stackable = isStackable(id);
			if (!stackable) {
				List<Effect> effects = target.getEffects();

				for (int i = 0; i < effects.size(); ++i) {
					Effect e = effects.get(i);
					if (e.attackID == attack_id) {
						target.removeEffect(e);
						break;
					}
				}
			}
			target.addEffect(effect);
			caster.addLaunchedEffect(effect);
		}

		// Compute the effect
		effect.apply(fight);
		effect.addLog(fight);
	}
	
	public static boolean isStackable(int type) {
		return type == TYPE_POISON || type == TYPE_SHACKLE_MP || type == TYPE_SHACKLE_TP 
				|| type == TYPE_SHACKLE_STRENGTH || type == TYPE_SHACKLE_MAGIC || type == TYPE_VULNERABILITY;
	}

	protected void addLog(Fight fight) {
		if (turns == 0) {
			return;
		}
		logID = ActionAddEffect.createEffect(fight.getActions(), attackType, attackID, caster, target, id, value, turns);
	}

	public Stats getStats() {
		return stats;
	}

	public int getID() {
		return id;
	}

	public int getLogID() {
		return logID;
	}

	public boolean isCritical() {
		return critical;
	}

	public int getTurns() {
		return turns;
	}

	public void setTurns(int turns) {
		this.turns = turns;
	}

	public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = power;
	}

	public double getValue1() {
		return value1;
	}

	public double getValue2() {
		return value2;
	}

	public Entity getCaster() {
		return caster;
	}

	public Entity getTarget() {
		return target;
	}

	public AbstractLeekValue getLeekValue(AI ai) throws Exception {

		ArrayLeekValue retour = new ArrayLeekValue();
		retour.get(ai, 0).set(ai, LeekValueManager.getLeekIntValue(id));
		retour.get(ai, 1).set(ai, LeekValueManager.getLeekIntValue(value));
		retour.get(ai, 2).set(ai, LeekValueManager.getLeekIntValue(caster.getFId()));
		retour.get(ai, 3).set(ai, LeekValueManager.getLeekIntValue(turns));
		retour.get(ai, 4).set(ai, LeekValueManager.getLeekBooleanValue(critical));
		retour.get(ai, 5).set(ai, LeekValueManager.getLeekIntValue(attackID));
		retour.get(ai, 6).set(ai, LeekValueManager.getLeekIntValue(target.getFId()));
		return retour;
	}

	// Abstract methods
	public void apply(Fight fight) {}

	public void applyStartTurn(Fight fight) {}

	public void reduce() {}
}