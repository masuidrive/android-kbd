package com.google.android.apps.inputmethod.libs.mozc.session;

/**
 * Exact class name registered by the upstream Mozc Android shared library.
 * Call {@link #initialize()} before using the methods registered by that call.
 */
public final class MozcJNI {
  private MozcJNI() {}

  public static native boolean initialize();

  public static native byte[] evalCommand(byte[] command);

  public static native boolean onPostLoad(String userProfileDirectory, String dataFilePath);

  public static native String getDataVersion();
}
