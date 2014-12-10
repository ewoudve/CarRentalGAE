package ds.gae.entities;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class TaskStatus implements Serializable{

	private static final long serialVersionUID = 1L;
	
	@Id
	private String renter;
	
	private String message;
	
	public TaskStatus(String renter, String message){
		this.renter = renter;
		this.message = message;
	}
	
	public String getRenter(){
		return this.renter;
	}
	
	public String getMessage(){
		return this.message;
	}

}
