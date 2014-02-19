package mock.net.cube.common;

import net.cube.token.ITokenPassphraseCallback;

public class MockTokenPassphraseCallback implements ITokenPassphraseCallback {
	private char[] pwd;

	public MockTokenPassphraseCallback(String string) {
		this.pwd = string.toCharArray();
	}

	@Override
	public char[] getPassphrase() {
		return pwd;
	}
}
