package embedded.treasurehunt.model;

import java.io.Serializable;
import java.util.List;

public class Treasure implements Serializable {
	private int id;
	private String name;
	private String description;
	
	private List<Hint> hints;
	
	public Treasure(){
		
	}
	
	public Treasure(String name, String description){
		this.name = name;
		this.description = description;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Hint> getHints() {
		return hints;
	}

	public void setHints(List<Hint> hints) {
		this.hints = hints;
	}
}
