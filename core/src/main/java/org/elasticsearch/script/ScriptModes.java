/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.script;

import org.apache.lucene.util.SetOnce;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Holds the boolean indicating the enabled mode for each of the different scripting languages available, each script source and each
 * scripted operation.
 */
public class ScriptModes {

    private static final String SCRIPT_SETTINGS_PREFIX = "script";
    private static final String ENGINE_SETTINGS_PREFIX = "script.engine";

    final Map<String, Boolean> scriptEnabled;

    private static final Setting<List<String>> TYPES_ALLOWED_SETTING =
        Setting.listSetting("script.types_allowed", Collections.emptyList(), Function.identity(), Setting.Property.NodeScope);
    private static final Setting<List<String>> CONTEXTS_ALLOWED_SETTING =
        Setting.listSetting("script.contexts_allowed", Collections.emptyList(), Function.identity(), Setting.Property.NodeScope);

    private final Set<String> typesAllowed;
    private final Set<String> contextsAllowed;

    ScriptModes(ScriptContextRegistry scriptContextRegistry, ScriptSettings scriptSettings, Settings settings) {
        HashMap<String, Boolean> scriptModes = new HashMap<>();
        for (Setting<Boolean> scriptModeSetting : scriptSettings.getScriptLanguageSettings()) {
            scriptModes.put(scriptModeSetting.getKey(), scriptModeSetting.get(settings));
        }
        this.scriptEnabled = Collections.unmodifiableMap(scriptModes);

        typesAllowed = TYPES_ALLOWED_SETTING.exists(settings) ? new HashSet<>() : null;

        if (typesAllowed != null) {
            for (String settingType : TYPES_ALLOWED_SETTING.get(settings)) {
                boolean found = false;

                for (ScriptType scriptType : ScriptType.values()) {
                    if (scriptType.getName().equals(settingType)) {
                        found = true;
                        typesAllowed.add(settingType);

                        break;
                    }
                }

                if (!found) {
                    throw new IllegalArgumentException(
                        "unknown script type [" + settingType + "] found in setting [" + TYPES_ALLOWED_SETTING.getKey() + "].");
                }
            }
        }

        contextsAllowed = CONTEXTS_ALLOWED_SETTING.exists(settings) ? new HashSet<>() : null;

        if (contextsAllowed != null) {
            for (String settingContext : CONTEXTS_ALLOWED_SETTING.get(settings)) {
                if (scriptContextRegistry.isSupportedContext(settingContext)) {
                    contextsAllowed.add(settingContext);
                } else {
                    throw new IllegalArgumentException(
                        "unknown script context [" + settingContext + "] found in setting [" + CONTEXTS_ALLOWED_SETTING.getKey() + "].");
                }
            }
        }
    }

    /**
     * Returns the script mode for a script of a certain written in a certain language,
     * of a certain type and executing as part of a specific operation/api.
     *
     * @param lang the language that the script is written in
     * @param scriptType the type of the script
     * @param scriptContext the operation that requires the execution of the script
     * @return whether scripts are enabled (true) or disabled (false)
     */
    public boolean getScriptEnabled(String lang, ScriptType scriptType, ScriptContext scriptContext) {
        if (typesAllowed != null && typesAllowed.contains(scriptType.getName()) == false) {
            throw new IllegalArgumentException("[" + scriptType.getName() + "] scripts cannot be executed");
        }

        if (contextsAllowed != null && contextsAllowed.contains(scriptContext.getKey()) == false) {
            throw new IllegalArgumentException("[" + scriptContext.getKey() + "] scripts cannot be executed");
        }

        Boolean scriptMode = scriptEnabled.get(getKey(lang, scriptType, scriptContext));
        if (scriptMode == null) {
            throw new IllegalArgumentException("script mode not found for lang [" + lang + "], script_type [" + scriptType + "], operation [" + scriptContext.getKey() + "]");
        }
        return scriptMode;
    }

    static String operationKey(ScriptContext scriptContext) {
        return SCRIPT_SETTINGS_PREFIX + "." + scriptContext.getKey();
    }

    static String sourceKey(ScriptType scriptType) {
        return SCRIPT_SETTINGS_PREFIX + "." + scriptType.getName();
    }

    static String getGlobalKey(String lang, ScriptType scriptType) {
        return ENGINE_SETTINGS_PREFIX + "." + lang + "." + scriptType;
    }

    static String getKey(String lang, ScriptType scriptType, ScriptContext scriptContext) {
        return ENGINE_SETTINGS_PREFIX + "." + lang + "." + scriptType + "." + scriptContext.getKey();
    }

    @Override
    public String toString() {
        //order settings by key before printing them out, for readability
        TreeMap<String, Boolean> scriptModesTreeMap = new TreeMap<>();
        scriptModesTreeMap.putAll(scriptEnabled);
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Boolean> stringScriptModeEntry : scriptModesTreeMap.entrySet()) {
            stringBuilder.append(stringScriptModeEntry.getKey()).append(": ").append(stringScriptModeEntry.getValue()).append("\n");
        }
        return stringBuilder.toString();
    }
}
