package dev.zwander.installwithoptions;

import dev.zwander.installwithoptions.IOptionsApplier;
import java.util.List;
import java.util.Map;

interface IShellInterface {
    void install(in Map descriptors, in int[] options, IOptionsApplier optionsApplier, String installerPackageName, int userId) = 1;
    List getUserIds() = 2;

    void destroy() = 16777114;
}