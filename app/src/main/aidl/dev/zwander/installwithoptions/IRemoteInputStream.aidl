package dev.zwander.installwithoptions;

import dev.zwander.installwithoptions.IRemoteOutputStream;

interface IRemoteInputStream {
    int read() = 1;
    void close() = 2;
}
