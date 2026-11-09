package com.bensler.taggy.persist.base;
import static java.util.function.Function.identity;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.bensler.decaf.util.entity.Entity;
import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.taggy.persist.base.PersistencyBaseLayer.PropertyTable;

public class EntityPropertyType<JAVA_TYPE, DB_PROPERTY_TYPE> {

  public static <JT> JT convertToSingleValue(List<Object> dbValues, Class<JT> clazz) {
    final int dbValuesCount = dbValues.size();

    if (dbValuesCount != 1) {
      throw new IllegalArgumentException("Exactly one value expected in param 'dbValues', %s found.".formatted(dbValuesCount));
    } else {
      return clazz.cast(dbValues.getFirst());
    }
  }

  public static <T> List<T> castCollection(List<Object> dbValues, Class<T> type) {
    return dbValues.stream().map(type::cast).toList();
  }

  public static final EntityPropertyType<String, String> STRING = createSingle(
    "STRING", PersistencyBaseLayer.PROPERTY_TABLE_STRING, identity(), dbValues -> convertToSingleValue(dbValues, String.class)
  );
  public static final EntityPropertyType<List<String>, String> STRINGS = createMulti(
    "STRINGS", PersistencyBaseLayer.PROPERTY_TABLE_STRING, List::stream, identity(), dbValues -> castCollection(dbValues, String.class)
  );
  public static final EntityPropertyType<Integer, Integer> INTEGER = createSingle(
    "INTEGER", PersistencyBaseLayer.PROPERTY_TABLE_INTEGER, identity(), dbValues -> convertToSingleValue(dbValues,Integer.class)
  );
  public static final EntityPropertyType<List<Integer>, Integer> INTEGERS = createMulti(
    "INTEGERS", PersistencyBaseLayer.PROPERTY_TABLE_INTEGER, List::stream, identity(), dbValues -> castCollection(dbValues, Integer.class)
  );
  public static final EntityPropertyType<String, String> BLOB = createSingle(
    "BLOB", PersistencyBaseLayer.PROPERTY_TABLE_BLOB, identity(), dbValues -> convertToSingleValue(dbValues, String.class)
  );
          static final EntityPropertyType<Integer, Integer> ENTITY = createSingle(
    "ENTITY", PersistencyBaseLayer.PROPERTY_TABLE_ENTITY, identity(), dbValues -> convertToSingleValue(dbValues, Integer.class)
  );
  public  static final EntityPropertyType<List<Integer>, Integer> ENTITIES = createMulti(
    "ENTITIES", PersistencyBaseLayer.PROPERTY_TABLE_ENTITY, List::stream, identity(), dbValues -> castCollection(dbValues, Integer.class)
  );

  public static <JT extends Entity<JT>> EntityPropertyType<EntityReference<JT>, Integer> createEntityType(Class<JT> entityClass, boolean lazy) {
    EntityPropertyType<EntityReference<JT>, Integer> type = createSingle(
      "ENTITY", PersistencyBaseLayer.PROPERTY_TABLE_ENTITY, EntityReference::getId,
      dbValues -> new EntityReference<>(entityClass, convertToSingleValue(dbValues, Integer.class))
    );
    return type;
  }

  private static <JT, DT> EntityPropertyType<JT, DT> createSingle(String name, PropertyTable<DT> table, Function<JT, DT> writeMapper, Function<List<Object>, JT> loadConverter) {
    return createMulti(name, table, javaValue -> Stream.of(javaValue), writeMapper, loadConverter);
  }

  private static <JT, IT, DT> EntityPropertyType<JT, DT> createMulti(
    String name, PropertyTable<DT> table,
    Function<JT, Stream<IT>> decomposer, Function<IT, DT> writeMapper,
    Function<List<Object>, JT> loadConverter
  ) {
    return new EntityPropertyType<>(
      name,
      (tableEntryCollector, javaValue) -> decomposer.apply(javaValue).forEach(
        tmpValue -> tableEntryCollector.accept(table, writeMapper.apply(tmpValue))
      ),
      loadConverter
    );
  }

  private final String name_;
  private final BiConsumer<BiConsumer<PropertyTable<DB_PROPERTY_TYPE>, DB_PROPERTY_TYPE>, JAVA_TYPE> saveConverter_;
  private final Function<List<Object>, JAVA_TYPE> loadConverter_;

  private EntityPropertyType(
    String name, BiConsumer<BiConsumer<PropertyTable<DB_PROPERTY_TYPE>, DB_PROPERTY_TYPE>, JAVA_TYPE> saveConverter,
    Function<List<Object>, JAVA_TYPE> loadConverter
  ) {
    name_ = name;
    saveConverter_ = saveConverter;
    loadConverter_ = loadConverter;
  }

  public void store(BiConsumer<PropertyTable<DB_PROPERTY_TYPE>, DB_PROPERTY_TYPE> tableEntryCollector, JAVA_TYPE value) {
    saveConverter_.accept(tableEntryCollector, value);
  }

  public JAVA_TYPE load(List<Object> dbValues) {
    return loadConverter_.apply(dbValues);
  }

  public String getName() {
    return name_;
  }

}
