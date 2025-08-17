package dev.zwander.installwithoptions;

interface IErrorCallback {
    void onError(String error, String errorClass) = 1;
}
