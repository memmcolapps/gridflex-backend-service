package org.memmcol.gridflexbackendservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Role implements Serializable {

	private static final long serialVersionUID = 1L;

//	@JsonProperty("RoleId")
	private Long roleId;
	    
//	@JsonProperty("OperatorRole")
	@NotBlank(message = "Operator role is required")
    private String operatorRole;

	public Long getRoleId() {
		return roleId;
	}

	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}

	public String getOperatorRole() {
		return operatorRole;
	}

	public void setOperatorRole(String operatorRole) {
		this.operatorRole = operatorRole;
	}
}
