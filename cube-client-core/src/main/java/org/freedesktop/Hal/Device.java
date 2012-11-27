package org.freedesktop.Hal;

import java.util.Map;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Variant;

public interface Device extends DBusInterface {
  public Map<String, Variant<Object>> GetAllProperties();

}
