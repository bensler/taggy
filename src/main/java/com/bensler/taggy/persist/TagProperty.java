package com.bensler.taggy.persist;

import org.hibernate.usertype.UserTypeSupport;

public enum TagProperty {

  REPRESENTED_DATE;

  public static class Converter extends UserTypeSupport<TagProperty> {

    public Converter() {super(TagProperty.class, 12);}

  }

}
