package dev.zwander.installwithoptions;

import java.util.List;

interface IShellInterface {
    void install(in List fileDescriptors, in List options, boolean splits) = 1;

    void destroy() = 16777114;
}