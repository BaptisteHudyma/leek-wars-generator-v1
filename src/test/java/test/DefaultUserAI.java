package test;
import leekscript.runner.AI;
import leekscript.runner.values.AbstractLeekValue;

public class DefaultUserAI extends AI {

	public DefaultUserAI() throws Exception {
		super();
	}

	@Override
	protected String[] getErrorString() {
		return null;
	}

	@Override
	protected String getAItring() {
		return null;
	}

	@Override
	public AbstractLeekValue runIA() throws Exception {
		return null;
	}

	@Override
	public int userFunctionCount(int id) {
		return 0;
	}

	@Override
	public boolean[] userFunctionReference(int id) {
		return null;
	}

	@Override
	public AbstractLeekValue userFunctionExecute(int id, AbstractLeekValue[] value) throws Exception {
		return null;
	}

	@Override
	public int anonymousFunctionCount(int id) {
		return 0;
	}

	@Override
	public boolean[] anonymousFunctionReference(int id) {
		return null;
	}

}
