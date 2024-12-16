package tmp;

import java.io.File;

import com.bensler.decaf.util.prefs.PrefKey;
import com.bensler.decaf.util.prefs.Prefs;

public class XmlTest {

  public static final PrefKey TAGGY = new PrefKey(PrefKey.ROOT, "taggy");
  public static final PrefKey BLA = new PrefKey(TAGGY, "bla");
  public static final PrefKey BLUB = new PrefKey(TAGGY, "blub");

  public static void main(String[] args) throws Exception {
    final Prefs prefs = new Prefs(new File(new File(System.getProperty("user.dir")), "x.xml"));

    System.out.println("bla: " + prefs.get(BLA, "nix"));
    System.out.println("blub: " + prefs.get(BLUB, "nix"));

    prefs.put(BLA, "blah");
    prefs.put(BLUB, "blubb");
    prefs.store();
  }

}
