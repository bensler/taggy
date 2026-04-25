package com.bensler.taggy.persist;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.PersistencyBaseLayer.PropertyTable;

public class EntityPropertyType<PROPERTY_TYPE, DB_PROPERTY_TYPE> {

  public static EntityPropertyType<String, String> STRING = new EntityPropertyType<>(
    "STRING", PersistencyBaseLayer.PROPERTY_TABLE_STRING, (string, consumer) -> consumer.accept(string)
  );
  public static EntityPropertyType<List<String>, String> STRINGS = new EntityPropertyType<>(
    "STRINGS", PersistencyBaseLayer.PROPERTY_TABLE_STRING, (strings, consumer) -> strings.stream().forEach(consumer::accept)
  );
  public static EntityPropertyType<Integer, Integer> INTEGER = new EntityPropertyType<>(
    "INTEGER", PersistencyBaseLayer.PROPERTY_TABLE_INTEGER, (anInt, consumer) -> consumer.accept(anInt)
  );
  public static EntityPropertyType<List<Integer>, Integer> INTEGERS = new EntityPropertyType<>(
    "INTEGERS", PersistencyBaseLayer.PROPERTY_TABLE_INTEGER, (ints, consumer) -> ints.stream().forEach(consumer::accept)
  );
  public static EntityPropertyType<EntityReference<?>, Integer> ENTITY = new EntityPropertyType<>(
    "ENTITY", PersistencyBaseLayer.PROPERTY_TABLE_ENTITY, (ref, consumer) -> consumer.accept(ref.getId())
  );
  public static EntityPropertyType<List<EntityReference<?>>, Integer> ENTITIES = new EntityPropertyType<>(
    "ENTITIES", PersistencyBaseLayer.PROPERTY_TABLE_ENTITY, (refs, consumer) -> refs.stream().map(EntityReference::getId).forEach(consumer::accept)
  );
  public static EntityPropertyType<String, String> BLOB = new EntityPropertyType<>(
    "BLOB", PersistencyBaseLayer.PROPERTY_TABLE_BLOB, (string, consumer) -> consumer.accept(string)
  );

  private final String name_;
  private final PropertyTable<DB_PROPERTY_TYPE> table_;
  private final BiConsumer<PROPERTY_TYPE, Consumer<DB_PROPERTY_TYPE>> writeMapper_;

//  private EntityPropertyType(String name, PropertyTable<PROPERTY_TYPE> table) {
//    this(name, table, (PROPERTY_TYPE value, Consumer<PROPERTY_TYPE> consumer) -> consumer.accept(value));
//  }

  private EntityPropertyType(String name, PropertyTable<DB_PROPERTY_TYPE> table, BiConsumer<PROPERTY_TYPE, Consumer<DB_PROPERTY_TYPE>> writeMapper) {
    name_= name;
    table_ = table;
    writeMapper_ = writeMapper;
  }

  public String getName() {
    return name_;
  }

}
