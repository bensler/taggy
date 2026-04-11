package com.bensler.taggy.persist;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.bensler.decaf.util.entity.EntityReference;

public class EntityPropertyType<JAVA_TYPE> {

  public static EntityPropertyType<String> STRING = new EntityPropertyType<>(
    "STRING", null, null
  );
  public static EntityPropertyType<List<String>> STRINGS = new EntityPropertyType<>(
    "STRINGS", null, null
  );
  public static EntityPropertyType<Integer> INTEGER = new EntityPropertyType<>(
    "INTEGER", null, null
  );
  public static EntityPropertyType<List<Integer>> INTEGERS = new EntityPropertyType<>(
    "INTEGERS", null, null
  );
  public static EntityPropertyType<EntityReference<?>> ENTITY = new EntityPropertyType<>(
    "ENTITY", null, null
  );
  public static EntityPropertyType<List<EntityReference<?>>> ENTITIES = new EntityPropertyType<>(
    "ENTITIES", null, null
  );
  public static EntityPropertyType<String> BLOB = new EntityPropertyType<>(
    "BLOB", null, null
  );

  private final String name_;
  private final BiConsumer<JAVA_TYPE, Map<String, String>> persister_;
  private final Function<Map<String, String>, JAVA_TYPE> reader_;

  private EntityPropertyType(String name, BiConsumer<JAVA_TYPE, Map<String, String>> persister, Function<Map<String, String>, JAVA_TYPE> reader) {
    name_= name;
    persister_ = persister;
    reader_ = reader;
  }

  public String getName() {
    return name_;
  }

}
