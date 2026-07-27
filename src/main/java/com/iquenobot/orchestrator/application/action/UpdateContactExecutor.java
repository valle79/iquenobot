package com.iquenobot.orchestrator.application.action;

import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateContactExecutor implements ActionExecutor {

    @Override
    public ActionType supportedActionType() { return ActionType.UPDATE_CONTACT; }

    @Override
    @Transactional
    public void execute(Decision decision, ProcessingContext context) {
        Map<String, Object> params = decision.getParameters();
        if (params == null || context.getContact() == null) {
            log.warn("UpdateContactExecutor called without parameters or contact");
            return;
        }

        var contact = context.getContact();

        if (params.containsKey("email")) {
            contact.setEmail((String) params.get("email"));
        }
        if (params.containsKey("phone")) {
            contact.setPhone((String) params.get("phone"));
        }
        if (params.containsKey("fullName")) {
            contact.setFullName((String) params.get("fullName"));
        }
        if (params.containsKey("company")) {
            contact.setCompany((String) params.get("company"));
        }
        if (params.containsKey("notes")) {
            contact.setNotes((String) params.get("notes"));
        }

        log.info("Contact updated: id={}", contact.getId());
    }
}
