
package com.forsrc.entity;

import java.util.List;
public class User {
  private String usename;

  public final String getUsename() {
    return usename;
  }

  public void setUsename(String value) {
    usename = value;
  }

  private List<Role> roles;

  public final List<Role> getRoles() {
    return roles;
  }

  public void setRoles(List<Role> value) {
    roles = value;
  }

}
