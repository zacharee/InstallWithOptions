package dev.zwander.installwithoptions;

import android.content.res.AssetFileDescriptor;
import java.util.List;

interface IShellInterface {
    void install(in AssetFileDescriptor[] descriptors, in int[] options, boolean splits) = 1;

    void destroy() = 16777114;
}