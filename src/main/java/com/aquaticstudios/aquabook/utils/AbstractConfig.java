package com.aquaticstudios.aquabook.utils;

import dev.dejvokep.boostedyaml.YamlDocument;

public interface AbstractConfig {
    void setup();

    YamlDocument getConfig();

    void reloadConfig();
}
