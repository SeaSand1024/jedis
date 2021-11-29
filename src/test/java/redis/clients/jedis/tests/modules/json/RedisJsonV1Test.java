package redis.clients.jedis.tests.modules.json;

import static org.junit.Assert.*;
import static redis.clients.jedis.json.Path.ROOT_PATH;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.tests.modules.RedisModuleCommandsTestBase;

public class RedisJsonV1Test extends RedisModuleCommandsTestBase {

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  /* A simple class that represents an object in real life */
  @SuppressWarnings("unused")
  private static class IRLObject {

    public String str;
    public boolean bool;

    public IRLObject() {
      this.str = "string";
      this.bool = true;
    }
  }

  @SuppressWarnings("unused")
  private static class FooBarObject {

    public String foo;
    public boolean fooB;
    public int fooI;
    public float fooF;
    public String[] fooArr;

    public FooBarObject() {
      this.foo = "bar";
      this.fooB = true;
      this.fooI = 6574;
      this.fooF = 435.345f;
      this.fooArr = new String[]{"a", "b", "c"};
    }
  }

  private static class Baz {

    private String quuz;
    private String grault;
    private String waldo;

    public Baz(final String quuz, final String grault, final String waldo) {
      this.quuz = quuz;
      this.grault = grault;
      this.waldo = waldo;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null) {
        return false;
      }
      if (getClass() != o.getClass()) {
        return false;
      }
      Baz other = (Baz) o;

      return Objects.equals(quuz, other.quuz)
          && Objects.equals(grault, other.grault)
          && Objects.equals(waldo, other.waldo);
    }
  }

  private static class Qux {

    private String quux;
    private String corge;
    private String garply;
    private Baz baz;

    public Qux(final String quux, final String corge, final String garply, final Baz baz) {
      this.quux = quux;
      this.corge = corge;
      this.garply = garply;
      this.baz = baz;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null) {
        return false;
      }
      if (getClass() != o.getClass()) {
        return false;
      }
      Qux other = (Qux) o;

      return Objects.equals(quux, other.quux)
          && Objects.equals(corge, other.corge)
          && Objects.equals(garply, other.garply)
          && Objects.equals(baz, other.baz);
    }
  }

  private final Gson gson = new Gson();

  @Test
  public void basicSetGetShouldSucceed() {

    // naive set with a path
//    client.jsonSet("null", null, ROOT_PATH);
    client.jsonSet("null", ROOT_PATH, (Object) null);
    assertNull(client.jsonGet("null", String.class, ROOT_PATH));

    // real scalar value and no path
    client.jsonSet("str", ROOT_PATH, "strong");
    assertEquals("strong", client.jsonGet("str"));

    // a slightly more complex object
    IRLObject obj = new IRLObject();
    client.jsonSet("obj", ROOT_PATH, obj);
    Object expected = gson.fromJson(gson.toJson(obj), Object.class);
    assertTrue(expected.equals(client.jsonGet("obj")));

    // check an update
    Path p = Path.of(".str");
    client.jsonSet("obj", p, "strung");
    assertEquals("strung", client.jsonGet("obj", String.class, p));
  }

  @Test
  public void setExistingPathOnlyIfExistsShouldSucceed() {
    client.jsonSet("obj", ROOT_PATH, new IRLObject());
    Path p = Path.of(".str");
    client.jsonSet("obj", p, "strangle", JsonSetParams.jsonSetParams().xx());
    assertEquals("strangle", client.jsonGet("obj", String.class, p));
  }

  @Test
  public void setNonExistingOnlyIfNotExistsShouldSucceed() {
    client.jsonSet("obj", ROOT_PATH, new IRLObject());
    Path p = Path.of(".none");
    client.jsonSet("obj", p, "strangle", JsonSetParams.jsonSetParams().nx());
    assertEquals("strangle", client.jsonGet("obj", String.class, p));
  }

  @Test
  public void setWithoutAPathDefaultsToRootPath() {
    client.jsonSet("obj1", ROOT_PATH, new IRLObject());
//    client.jsonSet("obj1", "strangle", JsonSetParams.jsonSetParams().xx());
    client.jsonSetLegacy("obj1", (Object) "strangle", JsonSetParams.jsonSetParams().xx());
    assertEquals("strangle", client.jsonGet("obj1", String.class, ROOT_PATH));
  }

  @Test
  public void setExistingPathOnlyIfNotExistsShouldFail() {
    client.jsonSet("obj", ROOT_PATH, new IRLObject());
    Path p = Path.of(".str");
    assertNull(client.jsonSet("obj", p, "strangle", JsonSetParams.jsonSetParams().nx()));
  }

  @Test
  public void setNonExistingPathOnlyIfExistsShouldFail() {
    client.jsonSet("obj", ROOT_PATH, new IRLObject());
    Path p = Path.of(".none");
    assertNull(client.jsonSet("obj", p, "strangle", JsonSetParams.jsonSetParams().xx()));
  }

  @Test(expected = JedisDataException.class)
  public void setException() {
    // should error on non root path for new key
    client.jsonSet("test", Path.of(".foo"), "bar");
  }

  @Test
  public void getMultiplePathsShouldSucceed() {
    // check multiple paths
    IRLObject obj = new IRLObject();
    client.jsonSet("obj", gson.toJson(obj));
    Object expected = gson.fromJson(gson.toJson(obj), Object.class);
    assertTrue(expected.equals(client.jsonGet("obj", Object.class, Path.of("bool"), Path.of("str"))));
  }

  @Test
  public void getMultiplePathsShouldSucceedWithLegacy() {
    // check multiple paths
    IRLObject obj = new IRLObject();
    client.jsonSetLegacy("obj", obj);
    Object expected = gson.fromJson(gson.toJson(obj), Object.class);
    assertTrue(expected.equals(client.jsonGet("obj", Object.class, Path.of("bool"), Path.of("str"))));
  }

  @Test
  public void toggle() {

    IRLObject obj = new IRLObject();
    client.jsonSetLegacy("obj", obj);

    Path pbool = Path.of(".bool");
    // check initial value
    assertTrue(client.jsonGet("obj", Boolean.class, pbool));

    // true -> false
    client.jsonToggle("obj", pbool);
    assertFalse(client.jsonGet("obj", Boolean.class, pbool));

    // false -> true
    client.jsonToggle("obj", pbool);
    assertTrue(client.jsonGet("obj", Boolean.class, pbool));

    // ignore non-boolean field
    Path pstr = Path.of(".str");
    try {
      client.jsonToggle("obj", pstr);
      fail("String not a bool");
    } catch (JedisDataException jde) {
      assertTrue(jde.getMessage().contains("not a bool"));
    }
    assertEquals("string", client.jsonGet("obj", String.class, pstr));
  }

  @Test(expected = JedisDataException.class)
  public void getException() {
    client.jsonSet("test", ROOT_PATH, "foo");
    client.jsonGet("test", String.class, Path.of(".bar"));
  }

  @Test
  public void delValidShouldSucceed() {
    // check deletion of a single path
    client.jsonSet("obj", ROOT_PATH, new IRLObject());
    assertEquals(1L, client.jsonDel("obj", Path.of(".str")));
    assertTrue(client.exists("obj"));

    // check deletion root using default root -> key is removed
    assertEquals(1L, client.jsonDel("obj"));
    assertFalse(client.exists("obj"));
  }

  @Test
  public void delNonExistingPathsAreIgnored() {
    client.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(0L, client.jsonDel("foobar", Path.of(".foo[1]")));
  }

  @Test
  public void typeChecksShouldSucceed() {
    assertNull(client.jsonType("foobar"));
    client.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertSame(Object.class, client.jsonType("foobar"));
    assertSame(Object.class, client.jsonType("foobar", ROOT_PATH));
    assertSame(String.class, client.jsonType("foobar", Path.of(".foo")));
    assertSame(int.class, client.jsonType("foobar", Path.of(".fooI")));
    assertSame(float.class, client.jsonType("foobar", Path.of(".fooF")));
    assertSame(List.class, client.jsonType("foobar", Path.of(".fooArr")));
    assertSame(boolean.class, client.jsonType("foobar", Path.of(".fooB")));
    assertNull(client.jsonType("foobar", Path.of(".fooErr")));
  }

  @Test
  public void mgetWithPathWithAllKeysExist() {
    Baz baz1 = new Baz("quuz1", "grault1", "waldo1");
    Baz baz2 = new Baz("quuz2", "grault2", "waldo2");
    Qux qux1 = new Qux("quux1", "corge1", "garply1", baz1);
    Qux qux2 = new Qux("quux2", "corge2", "garply2", baz2);

    client.jsonSetLegacy("qux1", qux1);
    client.jsonSetLegacy("qux2", qux2);

    List<Baz> allBaz = client.jsonMGet(Path.of("baz"), Baz.class, "qux1", "qux2");

    assertEquals(2, allBaz.size());

    Baz testBaz1 = allBaz.stream() //
        .filter(b -> b.quuz.equals("quuz1")) //
        .findFirst() //
        .orElseThrow(() -> new NullPointerException(""));
    Baz testBaz2 = allBaz.stream() //
        .filter(q -> q.quuz.equals("quuz2")) //
        .findFirst() //
        .orElseThrow(() -> new NullPointerException(""));

    assertEquals(baz1, testBaz1);
    assertEquals(baz2, testBaz2);
  }

  @Test
  public void mgetAtRootPathWithMissingKeys() {
    Baz baz1 = new Baz("quuz1", "grault1", "waldo1");
    Baz baz2 = new Baz("quuz2", "grault2", "waldo2");
    Qux qux1 = new Qux("quux1", "corge1", "garply1", baz1);
    Qux qux2 = new Qux("quux2", "corge2", "garply2", baz2);

    client.jsonSet("qux1", gson.toJson(qux1));
    client.jsonSet("qux2", gson.toJson(qux2));

    List<Qux> allQux = client.jsonMGet(Qux.class, "qux1", "qux2", "qux3");

    assertEquals(3, allQux.size());
    assertNull(allQux.get(2));
    allQux.removeAll(Collections.singleton(null));
    assertEquals(2, allQux.size());
  }

  @Test
  public void arrLen() {
    client.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(Long.valueOf(3), client.jsonArrLen("foobar", Path.of(".fooArr")));
  }

  @Test
  public void arrLenDefaultPath() {
    assertNull(client.jsonArrLen("array"));
    client.jsonSetLegacy("array", new int[]{1, 2, 3});
    assertEquals(Long.valueOf(3), client.jsonArrLen("array"));
  }

  @Test
  public void clearArray() {
    client.jsonSet("foobar", ROOT_PATH, new FooBarObject());

    Path arrPath = Path.of(".fooArr");
    assertEquals(Long.valueOf(3), client.jsonArrLen("foobar", arrPath));

    assertEquals(1L, client.jsonClear("foobar", arrPath));
    assertEquals(Long.valueOf(0), client.jsonArrLen("foobar", arrPath));

    // ignore non-array
    Path strPath = Path.of("foo");
    assertEquals(0L, client.jsonClear("foobar", strPath));
    assertEquals("bar", client.jsonGet("foobar", String.class, strPath));
  }

  @Test
  public void clearObject() {
    Baz baz = new Baz("quuz", "grault", "waldo");
    Qux qux = new Qux("quux", "corge", "garply", baz);

    client.jsonSet("qux", gson.toJson(qux));
    Path objPath = Path.of("baz");
    assertEquals(baz, client.jsonGet("qux", Baz.class, objPath));

    assertEquals(1L, client.jsonClear("qux", objPath));
    assertEquals(new Baz(null, null, null), client.jsonGet("qux", Baz.class, objPath));
  }

  @Test
  public void arrAppendSameType() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);

    client.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    assertEquals(Long.valueOf(6), client.jsonArrAppend("test_arrappend", Path.of(".b"), 4, 5, 6));

    Integer[] array = client.jsonGet("test_arrappend", Integer[].class, Path.of(".b"));
    assertArrayEquals(new Integer[]{1, 2, 3, 4, 5, 6}, array);
  }

  @Test
  public void arrAppendMultipleTypes() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);

    client.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    assertEquals(Long.valueOf(6), client.jsonArrAppend("test_arrappend", Path.of(".b"), "foo", true, null));

    Object[] array = client.jsonGet("test_arrappend", Object[].class, Path.of(".b"));

    // NOTE: GSon converts numeric types to the most accommodating type (Double)
    // when type information is not provided (as in the Object[] below)
    assertArrayEquals(new Object[]{1.0, 2.0, 3.0, "foo", true, null}, array);
  }

  @Test
  public void arrAppendMultipleTypesWithDeepPath() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);

    client.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    assertEquals(Long.valueOf(4), client.jsonArrAppend("test_arrappend", Path.of(".c.d"), "foo", true, null));

    Object[] array = client.jsonGet("test_arrappend", Object[].class, Path.of(".c.d"));
    assertArrayEquals(new Object[]{"ello", "foo", true, null}, array);
  }

  @Test
  public void arrAppendAgaintsEmptyArray() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: [] }}";
    JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);

    client.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    assertEquals(Long.valueOf(3), client.jsonArrAppend("test_arrappend", Path.of(".c.d"), "a", "b", "c"));

    String[] array = client.jsonGet("test_arrappend", String[].class, Path.of(".c.d"));
    assertArrayEquals(new String[]{"a", "b", "c"}, array);
  }

  @Test(expected = JedisDataException.class)
  public void arrAppendPathIsNotArray() {
    String json = "{ a: 'hello', b: [1, 2, 3], c: { d: ['ello'] }}";
    JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);

    client.jsonSet("test_arrappend", ROOT_PATH, jsonObject);
    client.jsonArrAppend("test_arrappend", Path.of(".a"), 1);
  }

  @Test(expected = JedisDataException.class)
  public void arrIndexAbsentKey() {
    client.jsonArrIndex("quxquux", ROOT_PATH, gson.toJson(new Object()));
  }

  @Test
  public void arrIndexWithInts() {
    client.jsonSet("quxquux", ROOT_PATH, new int[]{8, 6, 7, 5, 3, 0, 9});
    assertEquals(2L, client.jsonArrIndex("quxquux", ROOT_PATH, 7));
    assertEquals(-1L, client.jsonArrIndex("quxquux", ROOT_PATH, "7"));
  }

  @Test
  public void arrIndexWithStrings() {
    client.jsonSet("quxquux", ROOT_PATH, new String[]{"8", "6", "7", "5", "3", "0", "9"});
    assertEquals(2L, client.jsonArrIndex("quxquux", ROOT_PATH, "7"));
  }

  @Test
  public void arrIndexWithStringsAndPath() {
    client.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(1L, client.jsonArrIndex("foobar", Path.of(".fooArr"), "b"));
  }

  @Test(expected = JedisDataException.class)
  public void arrIndexNonExistentPath() {
    client.jsonSet("foobar", ROOT_PATH, new FooBarObject());
    assertEquals(1L, client.jsonArrIndex("foobar", Path.of(".barArr"), "x"));
  }

  @Test
  public void arrInsert() {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    JsonArray jsonArray = new Gson().fromJson(json, JsonArray.class);

    client.jsonSet("test_arrinsert", ROOT_PATH, jsonArray);
    assertEquals(8L, client.jsonArrInsert("test_arrinsert", ROOT_PATH, 1, "foo"));

    Object[] array = client.jsonGet("test_arrinsert", Object[].class, ROOT_PATH);

    // NOTE: GSon converts numeric types to the most accommodating type (Double)
    // when type information is not provided (as in the Object[] below)
    assertArrayEquals(new Object[]{"hello", "foo", "world", true, 1.0, 3.0, null, false}, array);
  }

  @Test
  public void arrInsertWithNegativeIndex() {
    String json = "['hello', 'world', true, 1, 3, null, false]";
    JsonArray jsonArray = new Gson().fromJson(json, JsonArray.class);

    client.jsonSet("test_arrinsert", ROOT_PATH, jsonArray);
    assertEquals(8L, client.jsonArrInsert("test_arrinsert", ROOT_PATH, -1, "foo"));

    Object[] array = client.jsonGet("test_arrinsert", Object[].class, ROOT_PATH);
    assertArrayEquals(new Object[]{"hello", "world", true, 1.0, 3.0, null, "foo", false}, array);
  }

  @Test
  public void testArrayPop() {
    client.jsonSet("arr", ROOT_PATH, new int[]{0, 1, 2, 3, 4});
    assertEquals(Long.valueOf(4), client.jsonArrPop("arr", Long.class, ROOT_PATH));
    assertEquals(Long.valueOf(3), client.jsonArrPop("arr", Long.class, ROOT_PATH, -1));
    assertEquals(Long.valueOf(2), client.jsonArrPop("arr", Long.class));
    assertEquals(Long.valueOf(0), client.jsonArrPop("arr", Long.class, ROOT_PATH, 0));
    assertEquals(Double.valueOf(1), client.jsonArrPop("arr"));
  }

  @Test
  public void arrTrim() {
    client.jsonSet("arr", ROOT_PATH, new int[]{0, 1, 2, 3, 4});
    assertEquals(Long.valueOf(3), client.jsonArrTrim("arr", ROOT_PATH, 1, 3));
    assertArrayEquals(new Integer[]{1, 2, 3}, client.jsonGet("arr", Integer[].class, ROOT_PATH));
  }

  @Test
  public void strAppend() {
    client.jsonSet("str", ROOT_PATH, "foo");
    assertEquals(6L, client.jsonStrAppend("str", ROOT_PATH, "bar"));
    assertEquals("foobar", client.jsonGet("str", String.class, ROOT_PATH));
    assertEquals(8L, client.jsonStrAppend("str", "ed"));
//    assertEquals("foobared", client.jsonGet("str", String.class));
    assertEquals("foobared", client.jsonGet("str"));
  }

  @Test
  public void strLen() {
    assertNull(client.jsonStrLen("str"));
    client.jsonSet("str", ROOT_PATH, "foo");
    assertEquals(Long.valueOf(3), client.jsonStrLen("str"));
    assertEquals(Long.valueOf(3), client.jsonStrLen("str", ROOT_PATH));
  }
}