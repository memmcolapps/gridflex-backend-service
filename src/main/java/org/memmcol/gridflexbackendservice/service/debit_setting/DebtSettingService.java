package org.memmcol.gridflexbackendservice.service.debit_setting;

import org.memmcol.gridflexbackendservice.model.debt_setting.LiabilityCause;
import org.memmcol.gridflexbackendservice.model.debt_setting.PercentageRange;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.Map;
import java.util.UUID;

public interface DebtSettingService {
    Map<String, Object> createLiabilityCause(LiabilityCause request);

    Map<String, Object> updateLiabilityCause(LiabilityCause request);

    Map<String, Object> getLiabilityCauses(String type);

    Map<String, Object> getLiabilityCause(UUID id, UUID lcVersionId);

    Map<String, Object> approveLiabilityCause(UUID liabilityCauseId, String approveStatus) throws MissingServletRequestParameterException;

    Map<String, Object> createPercentage(PercentageRange request);

    Map<String, Object> updatePercentage(PercentageRange request);

    Map<String, Object> getAllPercentages(String type);

    Map<String, Object> getPercentage(UUID id, UUID percentageVersionId);

    Map<String, Object> approvePercentage(UUID percentageId, String approveStatus) throws MissingServletRequestParameterException;

    Map<String, Object> liabilityCauseChangeState(UUID id, Boolean status);

    Map<String, Object> parcentageChangeState(UUID id, Boolean status);
}
