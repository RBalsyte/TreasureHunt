package embedded.treasurehunt.model;

public class TreasureUser {
	private int id;
	private String username;
	private Treasure treasure;
	private Hint hint;
	
	public TreasureUser(){
		
	}
	
	public TreasureUser(String username){
		this.username = username;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}

	public Treasure getTreasure() {
		return treasure;
	}
	public void setTreasure(Treasure treasure) {
		this.treasure = treasure;
	}

	public Hint getHint() {
		return hint;
	}
	public void setHint(Hint hint) {
		this.hint = hint;
	}
}
