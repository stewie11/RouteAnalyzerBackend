package rangefinder.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.routecommon.dto.TransitRequestDTO;

public class TransitTaskRequestDTO {
	
	public String getTaskCertificate() {
		return taskCertificate;
	}

	public void setTaskCertificate(String taskCertificate) {
		this.taskCertificate = taskCertificate;
	}

	public TransitRequestDTO getTransitRequestDTO() {
		return transitRequestDTO;
	}

	public void setTransitRequestDTO(TransitRequestDTO transitRequestDTO) {
		this.transitRequestDTO = transitRequestDTO;
	}

	private String taskCertificate;
	
	private TransitRequestDTO transitRequestDTO;
}
