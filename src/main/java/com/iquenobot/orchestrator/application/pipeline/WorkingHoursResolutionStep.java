package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.model.WorkingHours;
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
public class WorkingHoursResolutionStep implements PipelineStep, MessagePipeline.PrioritizedStep {

    private final SettingRepository settingRepository;

    @Override
    public int getOrder() { return 36; }

    @Override
    public ProcessingContext execute(ProcessingContext context) {
        if (context.getTenantId() == null) {
            context.setWorkingHours(WorkingHours.builder().enabled(false).build());
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

        String timezone = context.getTenant() != null ? context.getTenant().getTimezone() : null;
        WorkingHours workingHours = WorkingHours.fromSettings(settingsByKey, timezone);
        context.setWorkingHours(workingHours);

        log.debug("Working hours resolved: enabled={} isOpen={}", workingHours.isEnabled(), workingHours.isCurrentlyOpen());
        return context;
    }
}
