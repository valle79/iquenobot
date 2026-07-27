package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.orchestrator.domain.model.BotConfiguration;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import com.iquenobot.setting.domain.entity.Setting;
import com.iquenobot.setting.domain.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotConfigurationResolutionStep implements PipelineStep, MessagePipeline.PrioritizedStep {

    private final SettingRepository settingRepository;

    @Override
    public int getOrder() { return 35; }

    @Override
    public ProcessingContext execute(ProcessingContext context) {
        if (context.getTenantId() == null) {
            context.setBotConfiguration(BotConfiguration.defaults());
            return context;
        }

        Map<String, Setting> settingsByKey = settingRepository
                .findByTenantIdAndDeletedFalse(context.getTenantId())
                .stream()
                .collect(Collectors.toMap(
                        s -> s.getCategory() + "." + s.getKey(),
                        s -> s,
                        (a, b) -> a
                ));

        BotConfiguration config = BotConfiguration.fromSettings(settingsByKey);
        context.setBotConfiguration(config);

        log.debug("Bot configuration resolved: enabled={} aiProvider={}", config.isEnabled(), config.getAiProvider());
        return context;
    }
}
