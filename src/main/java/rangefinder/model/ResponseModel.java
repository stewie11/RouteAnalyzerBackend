package rangefinder.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseModel implements Serializable  {
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@JsonProperty("status")
	private int status;
	
	@JsonProperty("type")
	private int type;
	
	@JsonProperty("message")
	private String message;
}
