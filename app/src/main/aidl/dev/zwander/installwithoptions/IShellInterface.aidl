package dev.zwander.installwithoptions;

import dev.zwander.installwithoptions.IOptionsApplier;
import java.util.List;
import java.util.Map;

interface IShellInterface {
    void install(in Map descriptors, in int[] options, IOptionsApplier optionsApplier, String installerPackageName) = 1;

    void destroy() = 16777114;
}