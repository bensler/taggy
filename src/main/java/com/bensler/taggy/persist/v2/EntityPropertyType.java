package com.bensler.taggy.persist.v2;
import static java.util.function.Function.identity;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.bensler.taggy.persist.v2.PersistencyBaseLayer.PropertyTable;

public class EntityPropertyType<JAVA_TYPE, DB_PROPERTY_TYPE> {

  public static Object convertToSingleValue(Collection<Object> dbValues) {
    final int dbValuesCount = dbValues.size();

    if (dbValuesCount != 1) {
      throw new IllegalArgumentException("Exactly one value expected in param 'dbValues', %s found.".formatted(dbValuesCount));
    } else {
      return dbValues.stream().findFirst().get();
    }
  }

  public static <T> List<T> castCollection(Collection<Object> dbValues, Class<T> type) {
    return dbValues.stream().map(type::cast).toList();
  }

  public static EntityPropertyType<String, String> STRING = create(
    "STRING", PersistencyBaseLayer.PROPERTY_TABLE_STRING, identity(), dbValues -> String.class.cast(convertToSingleValue(dbValues))
  );
  public static EntityPropertyType<List<String>, String> STRINGS = create(
    "STRINGS", PersistencyBaseLayer.PROPERTY_TABLE_STRING, List::stream, identity(), dbValues -> castCollection(dbValues, String.class)
  );
  public static EntityPropertyType<Integer, Integer> INTEGER = create(
    "INTEGER", PersistencyBaseLayer.PROPERTY_TABLE_INTEGER, identity(), dbValues -> Integer.class.cast(convertToSingleValue(dbValues))
  );
  public static EntityPropertyType<List<Integer>, Integer> INTEGERS = create(
    "INTEGERS", PersistencyBaseLayer.PROPERTY_TABLE_INTEGER, List::stream, identity(), dbValues -> castCollection(dbValues, Integer.class)
  );
  public static EntityPropertyType<String, String> BLOB = create(
    "BLOB", PersistencyBaseLayer.PROPERTY_TABLE_BLOB, identity(), dbValues -> String.class.cast(convertToSingleValue(dbValues))
  );
  public static EntityPropertyType<Integer, Integer> ENTITY = create(
    "ENTITY", PersistencyBaseLayer.PROPERTY_TABLE_ENTITY, identity(), dbValues -> Integer.class.cast(convertToSingleValue(dbValues))
  );
  public static EntityPropertyType<List<Integer>, Integer> ENTITIES = create(
    "ENTITIES", PersistencyBaseLayer.PROPERTY_TABLE_ENTITY, List::stream, identity(), dbValues -> castCollection(dbValues, Integer.class)
  );

  private static <JT, DT> EntityPropertyType<JT, DT> create(String name, PropertyTable<DT> table, Function<JT, DT> writeMapper, Function<Collection<Object>, JT> loadConverter) {
    return create(name, table, javaValue -> Stream.of(javaValue), writeMapper, loadConverter);
  }

  private static <JT, IT, DT> EntityPropertyType<JT, DT> create(
    String name, PropertyTable<DT> table,
    Function<JT, Stream<IT>> decomposer, Function<IT, DT> writeMapper,
    Function<Collection<Object>, JT> loadConverter
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
  private final Function<Collection<Object>, JAVA_TYPE> loadConverter_;

  private EntityPropertyType(
    String name, BiConsumer<BiConsumer<PropertyTable<DB_PROPERTY_TYPE>, DB_PROPERTY_TYPE>, JAVA_TYPE> saveConverter,
    Function<Collection<Object>, JAVA_TYPE> loadConverter
  ) {
    name_= name;
    saveConverter_ = saveConverter;
    loadConverter_ = loadConverter;
  }

  public void store(BiConsumer<PropertyTable<DB_PROPERTY_TYPE>, DB_PROPERTY_TYPE> tableEntryCollector, JAVA_TYPE value) {
    saveConverter_.accept(tableEntryCollector, value);
  }

  public JAVA_TYPE load(Collection<Object> dbValues) {
    return loadConverter_.apply(dbValues);
  }

  public String getName() {
    return name_;
  }

}
