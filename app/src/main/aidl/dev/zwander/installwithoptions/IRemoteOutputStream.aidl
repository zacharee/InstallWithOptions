package dev.zwander.installwithoptions;

interface IRemoteOutputStream {
    void write(int b) = 1;
    void flush() = 2;
    void close() = 3;
}