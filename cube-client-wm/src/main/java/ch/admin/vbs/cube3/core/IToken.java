package ch.admin.vbs.cube3.core;

public interface IToken {
	public enum TokenEvent { INSERTED, REMOVED } 
	
	void addListener(ITokenListener l);
	void removeListener(ITokenListener l);
	
	interface ITokenListener {
		void tokenEvent(TokenEvent e);
	}
}
