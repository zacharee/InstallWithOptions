package dev.zwander.installwithoptions;

import android.content.pm.PackageInstaller.SessionParams;

interface IOptionsApplier {
    SessionParams applyOptions(in SessionParams params) = 1;
}
