package com.iquenobot.orchestrator.application.action;

import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TagConversationExecutor implements ActionExecutor {

    private final ConversationRepository conversationRepository;

    @Override
    public ActionType supportedActionType() { return ActionType.TAG_CONVERSATION; }

    @Override
    public void execute(Decision decision, ProcessingContext context) {
        String tag = decision.getParameters() != null
                ? (String) decision.getParameters().get("tag")
                : null;

        if (tag == null || tag.isBlank()) {
            log.warn("TagConversationExecutor called but no 'tag' parameter provided");
            return;
        }

        var conv = context.getConversation();
        String existingTags = conv.getTags();
        if (existingTags != null && !existingTags.isBlank()) {
            if (!existingTags.contains(tag)) {
                conv.setTags(existingTags + "," + tag);
            }
        } else {
            conv.setTags(tag);
        }

        conversationRepository.save(conv);
        log.info("Conversation tagged: id={} tag={}", conv.getId(), tag);
    }
}
