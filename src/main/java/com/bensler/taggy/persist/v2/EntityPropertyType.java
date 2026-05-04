package com.bensler.taggy.persist.v2;
import static java.util.function.Function.identity;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.v2.PersistencyBaseLayer.PropertyTable;

public class EntityPropertyType<JAVA_TYPE, DB_PROPERTY_TYPE> {

  public static EntityPropertyType<String, String> STRING = create(
    "STRING", PersistencyBaseLayer.PROPERTY_TABLE_STRING, identity()
  );
  public static EntityPropertyType<List<String>, String> STRINGS = create(
    "STRINGS", PersistencyBaseLayer.PROPERTY_TABLE_STRING, List::stream, identity()
  );
  public static EntityPropertyType<Integer, Integer> INTEGER = create(
    "INTEGER", PersistencyBaseLayer.PROPERTY_TABLE_INTEGER, identity()
  );
  public static EntityPropertyType<List<Integer>, Integer> INTEGERS = create(
    "INTEGERS", PersistencyBaseLayer.PROPERTY_TABLE_INTEGER, List::stream, identity()
  );
  public static EntityPropertyType<EntityReference<?>, Integer> ENTITY = create(
    "ENTITY", PersistencyBaseLayer.PROPERTY_TABLE_ENTITY, EntityReference::getId
  );
  public static EntityPropertyType<List<EntityReference<?>>, Integer> ENTITIES = create(
    "ENTITIES", PersistencyBaseLayer.PROPERTY_TABLE_ENTITY, List::stream, EntityReference::getId
  );
  public static EntityPropertyType<String, String> BLOB = create(
    "BLOB", PersistencyBaseLayer.PROPERTY_TABLE_BLOB, identity()
  );

  private static <JT, DT> EntityPropertyType<JT, DT> create(String name, PropertyTable<DT> table, Function<JT, DT> writeMapper) {
    return create(name, table, javaValue -> Stream.of(javaValue), writeMapper);
  }

  private static <JT, IT, DT> EntityPropertyType<JT, DT> create(
    String name, PropertyTable<DT> table,
    Function<JT, Stream<IT>> decomposer, Function<IT, DT> writeMapper
  ) {
    return new EntityPropertyType<>(
      name,
      (tableEntryCollector, javaValue) -> decomposer.apply(javaValue).forEach(
        tmpValue -> tableEntryCollector.accept(table, writeMapper.apply(tmpValue))
      )
    );
  }

  private final String name_;
  private final BiConsumer<BiConsumer<PropertyTable<DB_PROPERTY_TYPE>, DB_PROPERTY_TYPE>, JAVA_TYPE> valueConverter_;

  private EntityPropertyType(String name, BiConsumer<BiConsumer<PropertyTable<DB_PROPERTY_TYPE>, DB_PROPERTY_TYPE>, JAVA_TYPE> valueConverter) {
    name_= name;
    valueConverter_ = valueConverter;
  }

  public void store(BiConsumer<PropertyTable<DB_PROPERTY_TYPE>, DB_PROPERTY_TYPE> tableEntryCollector, JAVA_TYPE value) {
    valueConverter_.accept(tableEntryCollector, value);
  }

  public String getName() {
    return name_;
  }

}
