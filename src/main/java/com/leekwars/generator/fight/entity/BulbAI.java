package com.leekwars.generator.fight.entity;

import leekscript.runner.values.AbstractLeekValue;
import leekscript.runner.values.FunctionLeekValue;

public class BulbAI extends EntityAI {

	// private static final String TAG = SummonAI.class.getSimpleName();

	private FunctionLeekValue mAIFunction;
	private EntityAI mOwnerAI;

	public BulbAI(Entity entity, EntityAI owner_ai, FunctionLeekValue ai) throws Exception {
		super(entity, owner_ai.logs);
		mAIFunction = ai;
		setFight(owner_ai.fight);
		mBirthTurn = fight.getTurn();
		mOwnerAI = owner_ai;
	}

	@Override
	public AbstractLeekValue runIA() throws Exception {
		if (mAIFunction != null) {
			mOwnerAI.mEntity = mEntity;
			return mAIFunction.executeFunction(mOwnerAI, new AbstractLeekValue[] {});
		}
		return null;
	}
}
