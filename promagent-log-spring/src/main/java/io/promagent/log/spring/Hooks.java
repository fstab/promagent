package io.promagent.log.spring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Hooks {
    private Map<String, List<String>> annMethodHook;
    private Map<String, List<String>> annClassHook;
    private Map<String, List<String>> regHook;
}
