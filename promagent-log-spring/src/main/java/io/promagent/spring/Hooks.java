package io.promagent.spring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Hooks {
    private Map<String, List<String>> annotationMethodHook;
    private Map<String, List<String>> annotationClassHook;
    private Map<String, List<String>> regHooks;
}
