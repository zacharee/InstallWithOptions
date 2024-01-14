package dev.zwander.installwithoptions;

import android.net.Uri;
import dev.zwander.installwithoptions.IRemoteInputStream;
import dev.zwander.installwithoptions.IRemoteOutputStream;

interface IContentResolver {
    IRemoteInputStream openInputStream(in Uri uri) = 1;
    IRemoteOutputStream openOutputStream(in Uri uri) = 2;
}